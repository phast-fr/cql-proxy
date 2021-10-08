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

package com.phast.cql.proxy.providers

import org.hl7.fhir.r4.client.rest.RestClient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.opencds.cqf.cql.engine.retrieve.TerminologyAwareRetrieveProvider
import org.opencds.cqf.cql.engine.runtime.Code
import org.opencds.cqf.cql.engine.runtime.Interval
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class R4FhirRetrieveProvider(uri: String): TerminologyAwareRetrieveProvider() {

    private val fhirClient = RestClient(uri)

    private var credential: String? = null

    constructor(uri: String, credential: String) : this(uri) {
        this.credential = credential
        this.fhirClient.tokenType = "Bearer"
        this.fhirClient.credential = credential
    }

    override fun retrieve(
        context: String?,
        contextPath: String?,
        contextValue: Any?,
        dataType: String?,
        templateId: String?,
        codePath: String?,
        codes: Iterable<Code>?,
        valueSet: String?,
        datePath: String?,
        dateLowPath: String?,
        dateHighPath: String?,
        dateRange: Interval?
    ): MutableIterable<Any> {
        val resources = mutableListOf<Resource>()
        if (contextPath == "id"
            && contextValue is StringType
            && dataType != null) {
            val response = fhirClient
                .search()
                .withResourceType(dataType)
                .withId(contextValue.value)
                .execute()
                .block()
            response?.body?.entry?.forEach { entry ->
                entry.resource?.let { resources.add(it) }
            }
        }
        else if (contextPath == "subject"
            && contextValue is StringType
            && dataType != null
            && codes != null) {
            val response = fhirClient
                .search()
                .withResourceType(dataType)
                .withSubject(contextValue.value)
                .withCodes(codes)
                .execute()
                .block()
            response?.body?.entry?.forEach { entry ->
                entry.resource?.let { resources.add(it) }
            }
        }
        else if (contextPath == "subject"
            && contextValue is StringType
            && dataType != null
            && valueSet != null
            && !isExpandValueSets) {
            val response = fhirClient
                .search()
                .withResourceType(dataType)
                .withSubject(contextValue.value)
                .withValueSet(valueSet)
                .execute()
                .block()
            response?.body?.entry?.forEach { entry ->
                entry.resource?.let { resources.add(it) }
            }
        }
        else if (contextPath == "subject"
            && contextValue is StringType
            && dataType != null
            && valueSet != null
            && isExpandValueSets) {
            val response = fhirClient
                .search()
                .withResourceType(dataType)
                .withSubject(contextValue.value)
                .withCodes(terminologyProvider.expand(ValueSetInfo().withId(valueSet)))
                .execute()
                .block()
            response?.body?.entry?.forEach { entry ->
                entry.resource?.let { resources.add(it) }
            }
        }
        return resources
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(R4FhirRetrieveProvider::class.java)
    }
}
