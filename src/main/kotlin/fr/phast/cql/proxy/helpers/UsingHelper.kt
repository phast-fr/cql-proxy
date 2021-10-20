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

package fr.phast.cql.proxy.helpers

import org.apache.commons.lang3.tuple.Triple
import org.cqframework.cql.elm.execution.Library

object UsingHelper {

    // Returns a list of (Model, Version, Url) for the usings in library. The
    // "System" using is excluded.
    fun getUsingUrlAndVersion(usings: Library.Usings?): List<Triple<String, String, String>> {
        if (usings == null || usings.def == null) {
            return emptyList()
        }
        val usingDefs = mutableListOf<Triple<String, String, String>>()
        for (def in usings.def) {
            if (def.localIdentifier == "System") continue
            usingDefs.add(
                Triple.of(
                    def.localIdentifier, def.version,
                    urlsByModelName[def.localIdentifier]
                )
            )
        }
        return usingDefs
    }

    private val urlsByModelName: Map<String, String> = mapOf(
        "FHIR" to "http://hl7.org/fhir",
        "QDM" to "urn:healthit-gov:qdm:v5_4"
    )
}
