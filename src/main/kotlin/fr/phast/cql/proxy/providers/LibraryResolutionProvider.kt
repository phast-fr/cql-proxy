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

interface LibraryResolutionProvider<LibraryType> {

    fun resolveLibraryById(libraryId: String): LibraryType?

    fun resolveLibraryByName(libraryName: String, libraryVersion: String): LibraryType?

    fun resolveLibraryByCanonicalUrl(libraryUrl: String): LibraryType?

    fun update(library: LibraryType)

    // This function assumes that you're selecting from a set of libraries with the same name.
    // It returns the closest matching version, or the max version if no version is specified.
    fun <LibraryType> selectFromList(
        libraries: Iterable<LibraryType>,
        libraryVersion: String,
        getVersion: (library: LibraryType) -> String?
    ): LibraryType? {
        var library: LibraryType? = null
        var maxVersion: LibraryType? = null
        for (l in libraries) {
            val currentVersion = getVersion(l)
            if (currentVersion == libraryVersion) {
                library = l
            }
            if (maxVersion == null || compareVersions(
                    getVersion(maxVersion),
                    getVersion(l)
                ) < 0
            ) {
                maxVersion = l
            }
        }

        // If we were not given a version, return the highest found
        return library
    }

    fun compareVersions(version1: String?, version2: String?): Int {
        // Treat null as MAX VERSION
        if (version1 == null && version2 == null) {
            return 0
        }
        if (version1 != null && version2 == null) {
            return -1
        }
        if (version1 == null && version2 != null) {
            return 1
        }
        val string1Vals = version1!!.split("\\.").toTypedArray()
        val string2Vals = version2!!.split("\\.").toTypedArray()
        val length = string1Vals.size.coerceAtLeast(string2Vals.size)
        for (i in 0 until length) {
            val v1 = if (i < string1Vals.size) string1Vals[i].toInt() else 0
            val v2 = if (i < string2Vals.size) string2Vals[i].toInt() else 0

            //Making sure Version1 bigger than version2
            if (v1 > v2) {
                return 1
            }
            else if (v1 < v2) {
                return -1
            }
        }

        //Both are equal
        return 0
    }
}
