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

import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider
import org.cqframework.cql.cql2elm.LibrarySourceProvider
import org.hl7.elm.r1.VersionedIdentifier
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.InputStream


class LibrarySourceProvider<LibraryType, AttachmentType>(
    private val provider: LibraryResolutionProvider<LibraryType>,
    private val getAttachments: (library: LibraryType) -> Iterable<AttachmentType>?,
    private val getContentType: (attachment: AttachmentType) -> String?,
    private val getContent: (attachment: AttachmentType) -> ByteArray?
): LibrarySourceProvider {

    private val innerProvider = FhirLibrarySourceProvider()

    override fun getLibrarySource(versionedIdentifier: VersionedIdentifier): InputStream? {
        logger.info("get library source: ${versionedIdentifier.id}; ${versionedIdentifier.version}")
        try {
            val library = this.provider.resolveLibraryByName(
                versionedIdentifier.id,
                versionedIdentifier.version
            )

            library?.let<LibraryType, Iterable<AttachmentType>?> { library ->
                this.getAttachments(library)
            }?.forEach { attachment ->
                if (this.getContentType(attachment) == "text/cql") {
                    return ByteArrayInputStream(this.getContent(attachment))
                }
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
        return this.innerProvider.getLibrarySource(versionedIdentifier)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LibrarySourceProvider::class.java)
    }
}
