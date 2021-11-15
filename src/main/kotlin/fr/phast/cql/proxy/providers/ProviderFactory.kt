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

package fr.phast.cql.proxy.providers

import fr.phast.cql.engine.fhir.helper.FHIRHelpers
import fr.phast.cql.engine.fhir.model.R4FhirModelResolver
import fr.phast.cql.engine.fhir.retrieve.R4FhirRetrieveProvider
import fr.phast.cql.engine.fhir.terminology.R4FhirTerminologyProvider
import fr.phast.cql.services.decorators.CacheAwareTerminologyDecorator
import fr.phast.cql.services.providers.EvaluationProviderFactory
import org.hl7.fhir.r4.model.Endpoint
import org.opencds.cqf.cql.engine.data.CompositeDataProvider
import org.opencds.cqf.cql.engine.data.DataProvider
import org.opencds.cqf.cql.engine.data.ExternalFunctionProvider
import org.opencds.cqf.cql.engine.runtime.Code
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider
import org.springframework.stereotype.Component

@Component
class ProviderFactory(private val terminologyCache: MutableMap<String, Iterable<Code>>): EvaluationProviderFactory {

    override fun createDataProvider(
        model: String,
        version: String,
        uri: String,
        credential: String,
        terminologyProvider: TerminologyProvider
    ): DataProvider {
        if (model == "FHIR" && version.startsWith("4")) {
            val modelResolver = R4FhirModelResolver()
            val retrieveProvider = R4FhirRetrieveProvider(uri, credential)
            retrieveProvider.terminologyProvider = terminologyProvider
            retrieveProvider.isExpandValueSets = true
            return CompositeDataProvider(modelResolver, retrieveProvider)
        }
        throw IllegalArgumentException(
            String.format(
                "Can't construct a data provider for model %s version %s",
                model,
                version
            )
        )
    }

    override fun createDataProvider(
        model: String,
        version: String,
        endpoint: Endpoint,
        terminologyProvider: TerminologyProvider
    ): DataProvider {
        TODO("Not yet implemented")
    }

    override fun createTerminologyProvider(
        model: String, version: String, uri: String, credential: String?
    ): TerminologyProvider {
        if (model == "FHIR" && version.startsWith("4")) {
            if (uri.isNotEmpty()) {
                return CacheAwareTerminologyDecorator(R4FhirTerminologyProvider(uri, credential), terminologyCache)
            }
            throw IllegalArgumentException("Can't construct a terminology provider with empty URI")
        }
        throw IllegalArgumentException(
            String.format(
                "Can't construct a terminology provider for model %s version %s",
                model,
                version
            )
        )
    }

    override fun createTerminologyProvider(model: String, version: String, endpoint: Endpoint): TerminologyProvider {
        TODO("Not yet implemented")
    }

    override fun createExternalFunctionProvider(): ExternalFunctionProvider {
        return FHIRHelpers()
    }
}
