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

package com.phast.cql.proxy.resolver

import com.phast.cql.proxy.exception.UnknownTypeException
import org.opencds.cqf.cql.engine.runtime.Date
import org.hl7.fhir.r4.model.*
import org.opencds.cqf.cql.engine.model.ModelResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class R4FhirModelResolver: ModelResolver {

    private var packageName: String

    init {
        this.packageName = "org.hl7.fhir.r4.model"
    }

    override fun getPackageName(): String {
        return this.packageName
    }

    override fun setPackageName(packageName: String) {
        this.packageName = packageName
    }

    override fun resolvePath(target: Any, path: String?): Any? {
        when (target) {
            is Patient -> {
                return when (path) {
                    "birthDate.value" -> Date(target.birthDate?.value)
                    else -> {
                        logger.info("target: $target, path: $path")
                        null
                    }
                }
            }
            is Condition -> {
                return when (path) {
                    "verificationStatus" -> target.verificationStatus
                    "clinicalStatus" -> target.clinicalStatus
                    "abatement" -> {
                        return if (target.abatementString != null) {
                            target.abatementString
                        }
                        else if (target.abatementAge != null) {
                            target.abatementAge
                        }
                        else if (target.abatementPeriod != null) {
                            target.abatementPeriod
                        }
                        else if (target.abatementRange != null) {
                            target.abatementRange
                        }
                        else if (target.abatementDateTime != null) {
                            target.abatementDateTime
                        }
                        else {
                            null
                        }
                    }
                    else -> {
                        logger.info("target: $target, path: $path")
                        null
                    }
                }
            }
            is Observation -> {
                return when (path) {
                    "value" -> {
                        if (target.valueQuantity != null) {
                            target.valueQuantity
                        }
                        else if (target.valueString != null) {
                            target.valueString
                        }
                        else if (target.valueBoolean != null) {
                            target.valueBoolean
                        }
                        else if (target.valueInteger != null) {
                            target.valueInteger
                        }
                        else if (target.valuePeriod != null) {
                            target.valuePeriod
                        }
                        else if (target.valueCodeableConcept != null) {
                            target.valueCodeableConcept
                        }
                        else if (target.valueDateTime != null) {
                            target.valueDateTime
                        }
                        else if (target.valueRange != null) {
                            target.valueRange
                        }
                        else if (target.valueRatio != null) {
                            target.valueRatio
                        }
                        else if (target.valueSampledData != null) {
                            target.valueSampledData
                        }
                        else if (target.valueTime != null) {
                            target.valueTime
                        }
                        else {
                            null
                        }
                    }
                    "status" -> target.status
                    else -> {
                        logger.info("target: $target, path: $path")
                        null
                    }
                }
            }
            is CodeableConcept -> {
                return when (path) {
                    "coding" -> target.coding
                    "text" -> target.text
                    "display" -> target.text
                    else -> {
                        logger.info("target: $target, path: $path")
                        null
                    }
                }
            }
            is Coding -> {
                return when (path) {
                    "code" -> target.code
                    "system" -> target.system
                    "value" -> target.code
                    "version" -> target.version
                    "display" -> target.display
                    else -> {
                        logger.info("target: $target, path: $path")
                        null
                    }
                }
            }
            is Quantity -> {
                return when (path) {
                    "value" -> target.value
                    "comparator" -> target.comparator
                    "system" -> target.system
                    "unit" -> target.unit
                    "code" -> target.code
                    else -> {
                        logger.info("target: $target, path: $path")
                        null
                    }
                }
            }
            is StringType -> {
                return when (path) {
                    "value" -> target.value
                    else -> {
                        logger.info("target: $target, path: $path")
                        null
                    }
                }
            }
            is CodeType -> {
                return when (path) {
                    "value" -> target.value
                    else -> {
                        logger.info("target: $target, path: $path")
                        null
                    }
                }
            }
            is UriType -> {
                return when (path) {
                    "value" -> target.value
                    else -> {
                        logger.info("target: $target, path: $path")
                        null
                    }
                }
            }
            is DecimalType -> {
                return when (path) {
                    "value" -> target.value
                    else -> {
                        logger.info("target: $target, path: $path")
                        null
                    }
                }
            }
            is ObservationStatus -> {
                return when (path) {
                    "value" -> target.text
                    else -> {
                        logger.info("target: $target, path: $path")
                        null
                    }
                }
            }
        }

        logger.info("target: $target, path: $path")
        return null
    }

    override fun getContextPath(contextType: String?, targetType: String?): Any? {
        if (targetType == null || contextType == null) {
            return null
        }

        if (!(contextType == "Unspecified" || contextType == "Population")) {
            if (contextType == "Patient" && targetType == "MedicationStatement") {
                return "subject"
            }
            else if (contextType == targetType) {
                return "id"
            }
        }
        return "subject"
    }

    override fun resolveType(typeName: String?): Class<*> {
        try {
            // Other Types in package.
            return Class.forName(String.format("%s.%s", packageName, typeName))
        }
        catch (e: ClassNotFoundException) {
        }

        try {
            // Just give me SOMETHING.
            return Class.forName(typeName)
        }
        catch (e: ClassNotFoundException) {
            throw UnknownTypeException(
                String.format(
                    "Could not resolve type %s. Primary package for this resolver is %s",
                    typeName,
                    packageName
                )
            )
        }
    }

    override fun resolveType(value: Any): Class<*> {
        return value.javaClass
    }

    override fun `is`(value: Any?, type: Class<*>?): Boolean {
        TODO("Not yet implemented")
    }

    override fun `as`(value: Any?, type: Class<*>?, isStrict: Boolean): Any {
        TODO("Not yet implemented")
    }

    override fun createInstance(typeName: String?): Any {
        TODO("Not yet implemented")
    }

    override fun setValue(target: Any?, path: String?, value: Any?) {
        TODO("Not yet implemented")
    }

    override fun objectEqual(left: Any?, right: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun objectEquivalent(left: Any?, right: Any?): Boolean {
        TODO("Not yet implemented")
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(R4FhirModelResolver::class.java)
    }
}
