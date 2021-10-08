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

package com.phast.cql.proxy.loaders

import com.phast.cql.proxy.helpers.TranslatorHelper.errorsToString
import com.phast.cql.proxy.helpers.TranslatorHelper.getTranslator
import com.phast.cql.proxy.helpers.TranslatorHelper.readLibrary
import org.cqframework.cql.cql2elm.*
import org.cqframework.cql.elm.execution.Library
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.opencds.cqf.cql.engine.execution.LibraryLoader
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import javax.xml.bind.JAXBException

class LibraryLoader(private val libraryManager: LibraryManager,
                    private val modelManager: ModelManager): LibraryLoader {

    private val libraries = mutableMapOf<String, Library>()

    fun getLibraryManager(): LibraryManager {
        return libraryManager
    }

    fun getModelManager(): ModelManager {
        return modelManager
    }

    override fun load(libraryIdentifier: VersionedIdentifier?): Library {
        return resolveLibrary(libraryIdentifier)
    }

    private fun resolveLibrary(libraryIdentifier: VersionedIdentifier?): Library {
        requireNotNull(libraryIdentifier) { "Library identifier is null." }
        requireNotNull(libraryIdentifier.id) { "Library identifier id is null." }
        val mangledId = mangleIdentifier(libraryIdentifier)
        var library: Library? = libraries[mangledId]
        if (library == null) {
            library = loadLibrary(libraryIdentifier)
            libraries[mangledId] = library
        }
        return library
    }

    private fun mangleIdentifier(libraryIdentifier: VersionedIdentifier): String {
        val id = libraryIdentifier.id
        val version = libraryIdentifier.version
        return if (version == null) id else "$id-$version"
    }

    private fun loadLibrary(libraryIdentifier: VersionedIdentifier): Library {
        val identifier = org.hl7.elm.r1.VersionedIdentifier()
            .withId(libraryIdentifier.id).withSystem(libraryIdentifier.system)
            .withVersion(libraryIdentifier.version)
        val errors = ArrayList<CqlTranslatorException>()
        val translatedLibrary =
            libraryManager.resolveLibrary(identifier, CqlTranslatorOptions.defaultOptions(), errors).library
        if (CqlTranslatorException.HasErrors(errors)) {
            throw IllegalArgumentException(errorsToString(errors))
        }
        return try {
            val translator: CqlTranslator = getTranslator("", libraryManager, modelManager)
            if (translator.errors.size > 0) {
                throw IllegalArgumentException(errorsToString(translator.errors))
            }
            readLibrary(
                ByteArrayInputStream(
                    CqlTranslator.convertToXml(translatedLibrary).toByteArray(StandardCharsets.UTF_8)
                )
            )
        }
        catch (e: JAXBException) {
            throw IllegalArgumentException(
                String.format(
                    "Errors occurred translating library %s%s.",
                    identifier.id, if (identifier.version != null) "-" + identifier.version else ""
                )
            )
        }
    }
}
