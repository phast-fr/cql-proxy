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

import fr.phast.cql.proxy.loaders.LibraryLoader
import fr.phast.cql.proxy.providers.LibraryResolutionProvider
import fr.phast.cql.proxy.providers.LibrarySourceProvider
import org.cqframework.cql.cql2elm.LibraryManager
import org.cqframework.cql.cql2elm.ModelManager
import org.hl7.fhir.r4.model.Library

object LibraryHelper {

    fun createLibraryLoader(provider: LibraryResolutionProvider<Library>): LibraryLoader {
        val modelManager = ModelManager()
        val libraryManager = LibraryManager(modelManager)
        libraryManager.librarySourceLoader.clearProviders()
        libraryManager.librarySourceLoader.registerProvider(
            LibrarySourceProvider(provider,
                { x -> x.content?.asIterable() }, { x -> x.contentType?.value }) { x -> x.data?.toByteArray() })
        return LibraryLoader(libraryManager, modelManager)
    }
}
