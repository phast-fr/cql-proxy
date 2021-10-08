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
import org.hl7.fhir.r4.client.rest.exception.ResourceNotFoundException
import org.hl7.fhir.r4.model.*
import org.opencds.cqf.cql.engine.runtime.Code
import org.opencds.cqf.cql.engine.terminology.CodeSystemInfo
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo

class R4FhirTerminologyProvider(uri: String): TerminologyProvider {

    private val fhirClient = RestClient(uri)

    constructor(uri: String, credential: String?) : this(uri) {
        this.fhirClient.tokenType = "Basic"
        this.fhirClient.credential = credential
    }

    override fun `in`(code: Code?, valueSet: ValueSetInfo?): Boolean {
        TODO("Not yet implemented")
    }

    override fun expand(valueSet: ValueSetInfo): MutableIterable<Code> {
        if (!resolveByUrl(valueSet)) {
            return mutableListOf()
        }
        val codes = mutableListOf<Code>()
        val response = fhirClient
            .operation(ValueSet::class.java)
            .operationName("\$expand")
            .resourceType("ValueSet")
            .resourceId(valueSet.id)
            .execute()
            .block()
        response?.body?.expansion?.contains?.forEach { valueSetContains ->
           codes.add(
               Code()
                   .withCode(valueSetContains.code?.value)
                   .withSystem(valueSetContains.system?.value)
                   .withVersion(valueSetContains.version?.value)
                   .withDisplay(valueSetContains.display?.value)
           )
        }
        return codes
    }

    override fun lookup(code: Code?, codeSystem: CodeSystemInfo?): Code {
        TODO("Not yet implemented")
    }

    private fun resolveByUrl(valueSet: ValueSetInfo): Boolean {
        if (valueSet.version != null
            || valueSet.codeSystems != null && valueSet.codeSystems.size > 0
        ) {
            throw UnsupportedOperationException(
                String.format(
                    "Could not expand value set %s; version and code system bindings are not supported at this time.",
                    valueSet.id
                )
            )
        }

        var searchResults = fhirClient
            .search()
            .withResourceType("ValueSet")
            .withUrl(valueSet.id)
            .execute()
            .block()
            ?.body
        if (searchResults?.total?.value == 0) {
            searchResults = fhirClient
                .search()
                .withResourceType("ValueSet")
                .withId(valueSet.id)
                .execute()
                .block()
                ?.body
            if (searchResults?.total?.value == 0) {
                var id = valueSet.id
                if (id.startsWith(URN_OID)) {
                    id = id.replace(URN_OID, "")
                }
                else if (id.startsWith(URN_UUID)) {
                    id = id.replace(URN_UUID, "")
                }
                searchResults = Bundle(BundleType.SEARCHSET)
                // If we reached this point and it looks like it might
                // be a FHIR resource ID, we will try to read it.
                // See https://www.hl7.org/fhir/datatypes.html#id
                if (id.matches(Regex("[A-Za-z0-9\\-\\.]{1,64}"))) {
                    try {
                        val vs = fhirClient
                            .read(ValueSet::class.java)
                            .resourceType("ValueSet")
                            .resourceId(id)
                            .execute()
                            .block()
                            ?.body
                        searchResults.entry = listOf(BundleEntry().also { entry ->
                            entry.resource = vs
                        })
                        searchResults.total = UnsignedIntType(1)
                    }
                    catch (e: ResourceNotFoundException) {
                        // intentionally empty
                    }
                }
            }
        }
        if (searchResults != null && searchResults.total?.value == 0) {
            require(searchResults.entry != null && searchResults.entry!!.isNotEmpty()) {
                String.format("Could not resolve value set %s.", valueSet.id)
            }
            if (searchResults.total?.value == 1) {
                valueSet.id = searchResults.entry!![0].resource?.id?.value
            }
            else {
                throw IllegalArgumentException("Found more than 1 ValueSet with url: " + valueSet.id)
            }
        }
        return true
    }

    companion object {
        private const val URN_UUID = "urn:uuid:"

        private const val URN_OID = "urn:oid:"

    }
}
