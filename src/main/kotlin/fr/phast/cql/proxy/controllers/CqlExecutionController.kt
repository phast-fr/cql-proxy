/*
 * MIT License
 *
 * Copyright (c) 2021 PHAST
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fr.phast.cql.proxy.controllers

import fr.phast.cql.proxy.helpers.LibraryHelper
import fr.phast.cql.proxy.helpers.TranslatorHelper
import fr.phast.cql.proxy.helpers.TranslatorHelper.getTranslator
import fr.phast.cql.proxy.helpers.UsingHelper
import fr.phast.cql.proxy.providers.EvaluationProviderFactory
import fr.phast.cql.proxy.providers.LibraryResourceProvider
import org.apache.commons.lang3.tuple.Triple
import org.cqframework.cql.cql2elm.CqlTranslator
import org.cqframework.cql.elm.execution.FunctionDef
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.hl7.fhir.r4.model.*
import org.opencds.cqf.cql.engine.execution.Context
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/r4/fhir")
class CqlExecutionController(
    private val providerFactory: EvaluationProviderFactory
): HealthIndicator {

    override fun health(): Health {
        return Health.up().build()
    }

    @PostMapping("\$cql")
    fun doPostCql(@RequestBody parameters: Parameters): Bundle {
        val bundle = Bundle(BundleType.COLLECTION)
        if (parameters.parameter != null) {
            val codeParameter = parameters.parameter!!.find { parameter ->
                "code" == parameter.name.value
            }
            val code = codeParameter?.valueString!!

            val patientParameter = parameters.parameter!!.find { parameter ->
                "patientId" == parameter.name.value
            }
            val patientId = patientParameter?.valueString

            val libraryServiceUriParameter = parameters.parameter!!.find { parameter ->
                "libraryServiceUri" == parameter.name.value
            }
            val libraryServiceUri = libraryServiceUriParameter?.valueString!!

            val libraryCredentialParameter = parameters.parameter!!.find { parameter ->
                "libraryCredential" == parameter.name.value
            }
            val libraryCredential = libraryCredentialParameter?.valueString

            val terminologyServiceUriParameter = parameters.parameter!!.find { parameter ->
                "terminologyServiceUri" == parameter.name.value
            }
            val terminologyServiceUri = terminologyServiceUriParameter?.valueString!!

            val terminologyCredentialParameter = parameters.parameter!!.find { parameter ->
                "terminologyCredential" == parameter.name.value
            }
            val terminologyCredential = terminologyCredentialParameter?.valueString

            val dataServiceUriParameter = parameters.parameter!!.find { parameter ->
                "dataServiceUri" == parameter.name.value
            }
            val dataServiceUri = dataServiceUriParameter?.valueString!!

            val dataServiceTokenParameter = parameters.parameter!!.find { parameter ->
                "dataServiceAccessToken" == parameter.name.value
            }
            val dataServiceToken = dataServiceTokenParameter?.valueString!!

            val libraryResolutionProvider = LibraryResourceProvider(
                libraryServiceUri.value,
                Library::class.java,
                libraryCredential?.value
            )
            val translator: CqlTranslator?
            val libraryLoader = LibraryHelper.createLibraryLoader(libraryResolutionProvider)

            val results = mutableListOf<BundleEntry>()

            try {
                translator = getTranslator(
                    code.value, libraryLoader.getLibraryManager(), libraryLoader.getModelManager()
                )
                if (translator.errors.isNotEmpty()) {
                    translator.errors.forEach { cte ->
                        val result = Parameters()
                        val parametersParameter = mutableListOf<ParametersParameter>()
                        val tb = cte.locator
                        if (tb != null) {
                            val location = String.format("[%d:%d]", tb.startLine, tb.startChar)
                            parametersParameter.add(ParametersParameter(StringType("location")).also { parametersParameter ->
                                parametersParameter.valueString = StringType(location)
                            })
                        }
                        result.id = IdType("Error")
                        parametersParameter.add(ParametersParameter(StringType("error")).also { parametersParameter ->
                            parametersParameter.valueString = cte.message?.let { message ->
                                StringType(message)
                            }
                        })
                        result.parameter = parametersParameter

                        val bundleEntry = BundleEntry()
                        bundleEntry.fullUrl = UriType(result.id!!.value)
                        bundleEntry.resource = result
                        results.add(bundleEntry)
                    }
                    bundle.entry = results
                    bundle.total = UnsignedIntType(results.size)
                    return bundle
                }
            }
            catch (e: IllegalArgumentException ) {
                val result = Parameters()
                val parametersParameter = mutableListOf<ParametersParameter>()
                result.id = IdType("Error")
                parametersParameter.add(ParametersParameter(StringType("error")).also { parametersParameter ->
                    parametersParameter.valueString = e.message?.let { message ->
                        StringType(message)
                    }
                })
                result.parameter = parametersParameter

                val bundleEntry = BundleEntry()
                bundleEntry.fullUrl = UriType(result.id!!.value)
                bundleEntry.resource = result
                results.add(bundleEntry)

                bundle.entry = results
                bundle.total = UnsignedIntType(results.size)
                return bundle
            }

            val locations = getLocations(translator.translatedLibrary.library)

            val library: org.cqframework.cql.elm.execution.Library = TranslatorHelper.translateLibrary(translator)
            val context = Context(library)
            context.registerLibraryLoader(libraryLoader)

            val usingDefs: List<Triple<String, String, String>> = UsingHelper.getUsingUrlAndVersion(library.usings)

            require(usingDefs.size <= 1)
            { "Evaluation of Measure using multiple Models is not supported at this time." }

            // If there are no Usings, there is probably not any place the Terminology
            // actually used so I think the assumption that at least one provider exists is
            // ok.
            val terminologyProvider = if (usingDefs.isNotEmpty()) {
                // Creates a terminology provider based on the first using statement. This
                // assumes the terminology
                // server matches the FHIR version of the CQL.
                this.providerFactory.createTerminologyProvider(
                    usingDefs[0].left, usingDefs[0].middle,
                    terminologyServiceUri.value, terminologyCredential?.value
                ).also { terminologyProvider ->
                    context.registerTerminologyProvider(terminologyProvider)
                }
            }
            else {
                null
            }

            usingDefs.forEach { def ->
                terminologyProvider?.let { terminologyProvider ->
                    providerFactory.createDataProvider(
                        def.left!!, def.middle!!,
                        dataServiceUri.value, dataServiceToken.value,
                        terminologyProvider
                    ).also { dataProvider -> context.registerDataProvider(def.right, dataProvider) }
                }
            }

            val identifier = VersionedIdentifier()
            identifier.id = "FHIRHelpers"
            identifier.version = "4.0.1"
            context.registerExternalFunctionProvider(
                identifier, providerFactory.createExternalFunctionProvider()
            )

            context.setExpressionCaching(true)
            if (library.statements != null) {
                library.statements.def.forEach { def ->
                    context.enterContext(def.context)
                    if (patientId != null && patientId.value.isNotEmpty()) {
                        context.setContextValue(context.currentContext, patientId)
                    }
                    else {
                        context.setContextValue(context.currentContext, "null")
                    }

                    val result = Parameters()
                    val parametersParameter = mutableListOf<ParametersParameter>()
                    try {
                        result.id = IdType(def.name)
                        val location = String.format(
                            "[%d:%d]", locations[def.name]!![0],
                            locations[def.name]!![1]
                        )
                        parametersParameter.add(ParametersParameter(StringType("location")).also {
                            it.valueString = StringType(location)
                        })

                        if (def is FunctionDef) {
                            return@forEach
                        }
                        val res = def.expression.evaluate(context)
                        if (res == null) {
                            parametersParameter.add(ParametersParameter(StringType("value")).also {
                                it.valueString = StringType("null")
                            })
                        }
                        else if (res is List<*>) {
                            if (res.isNotEmpty() && res[0] is Resource) {
                                parametersParameter.add(ParametersParameter(StringType("value")).also { parameter ->
                                    parameter.resource = Bundle(BundleType.COLLECTION).also { bundle ->
                                        val entries = mutableListOf<BundleEntry>()
                                        (res as Iterable<*>).forEach { resource ->
                                            entries.add(BundleEntry().also { entry ->
                                                if (resource is Resource) {
                                                    entry.resource = resource
                                                    // TODO add resourceType
                                                    entry.fullUrl = resource.id?.let { UriType(it.value) }
                                                }
                                            })
                                        }
                                        bundle.total = UnsignedIntType(entries.size)
                                        bundle.entry = entries
                                    }
                                })
                            }
                            else {
                                parametersParameter.add(ParametersParameter(StringType("value")).also {
                                    it.valueString = StringType(res.toString())
                                })
                            }
                        }
                        else if (res is Iterable<*>) {
                            parametersParameter.add(ParametersParameter(StringType("value")).also { parameter ->
                                parameter.resource = Bundle(BundleType.COLLECTION).also { bundle ->
                                    val entries = mutableListOf<BundleEntry>()
                                    res.forEach { resource ->
                                        entries.add(BundleEntry().also { entry ->
                                            if (resource is Resource) {
                                                entry.resource = resource
                                                // TODO add resourceType
                                                entry.fullUrl = resource.id?.let { UriType(it.value) }
                                            }
                                        })
                                    }
                                    bundle.total = UnsignedIntType(entries.size)
                                    bundle.entry = entries
                                }
                            })
                        }
                        else if (res is Resource) {
                            parametersParameter.add(ParametersParameter(StringType("value")).also {
                                it.resource = res
                            })
                        }
                        else {
                            parametersParameter.add(ParametersParameter(StringType("value")).also {
                                it.valueString = StringType(res.toString())
                            })
                        }
                        parametersParameter.add(ParametersParameter(StringType("resultType")).also {
                            it.valueString = StringType(resolveType(res))
                        })
                    }
                    catch (re: RuntimeException) {
                        re.printStackTrace()
                        val message = if (re.message != null) re.message!! else re.javaClass.name
                        parametersParameter.add(ParametersParameter(StringType("error")).also {
                            it.valueString = StringType(message)
                        })
                    }
                    result.parameter = parametersParameter

                    val bundleEntry = BundleEntry()
                    bundleEntry.fullUrl = result.id?.let { UriType(it.value) }
                    bundleEntry.resource = result
                    results.add(bundleEntry)
                }

                bundle.entry = results
                bundle.total = UnsignedIntType(results.size)
            }
        }
        return bundle
    }

    private fun getLocations(library: org.hl7.elm.r1.Library): Map<String, List<Int>> {
        val locations = mutableMapOf<String, List<Int>>()
        if (library.statements == null) return locations
        for (def in library.statements.def) {
            val startLine = if (def.trackbacks.isEmpty()) 0 else def.trackbacks[0].startLine
            val startChar = if (def.trackbacks.isEmpty()) 0 else def.trackbacks[0].startChar
            val loc = listOf(startLine, startChar)
            locations[def.name] = loc
        }
        return locations
    }

    private fun resolveType(result: Any?): String {
        val type = if (result == null) "Null" else result.javaClass.simpleName
        when (type) {
            "BigDecimal" -> return "Decimal"
            "ArrayList" -> return "List"
            "FhirBundleCursor" -> return "Retrieve"
        }
        return type
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(CqlExecutionController::class.java)
    }
}
