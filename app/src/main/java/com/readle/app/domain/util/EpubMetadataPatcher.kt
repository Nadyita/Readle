package com.readle.app.domain.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Patches EPUB metadata to make series information compatible with Pocketbook devices.
 *
 * Problem: Pocketbook doesn't recognize EPUB 3.0 series metadata when <meta property="...">
 * tags lack the opf: namespace prefix.
 *
 * Solution: Add namespace declarations to <metadata> and convert <meta property="...">
 * tags to <opf:meta property="...">.
 */
class EpubMetadataPatcher {
    companion object {
        private const val TAG = "EpubMetadataPatcher"

        /**
         * Patches an EPUB file to make series metadata compatible with Pocketbook.
         * Creates a new file with "_patched" suffix.
         * 
         * @param cleanedTitle Optional cleaned title from the app database (with article at end)
         */
        suspend fun patchEpubForPocketbook(
            epubFile: File,
            cleanedTitle: String? = null
        ): Result<File> = withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting EPUB patch for: ${epubFile.name}")

                // Find OPF file location
                val opfPath = findOPFPath(epubFile)
                if (opfPath == null) {
                    Log.e(TAG, "Could not find OPF file in EPUB")
                    return@withContext Result.failure(Exception("OPF file not found in EPUB"))
                }
                Log.d(TAG, "Found OPF file at: $opfPath")

                // Read and patch OPF content
                val originalOpfContent = readFileFromZip(epubFile, opfPath)
                if (originalOpfContent == null) {
                    Log.e(TAG, "Could not read OPF file")
                    return@withContext Result.failure(Exception("Could not read OPF file"))
                }

                val patchedOpfContent = patchOPFContent(originalOpfContent, cleanedTitle)

                // Check if any changes were made
                if (patchedOpfContent == originalOpfContent) {
                    Log.d(TAG, "No changes needed, EPUB is already compatible")
                    return@withContext Result.success(epubFile)
                }

                // Create patched EPUB file
                val patchedFile = File(epubFile.parentFile, 
                    epubFile.nameWithoutExtension + "_patched.epub")
                
                createPatchedEpub(epubFile, patchedFile, opfPath, patchedOpfContent)

                Log.d(TAG, "Successfully patched EPUB: ${patchedFile.name}")
                Result.success(patchedFile)
            } catch (e: Exception) {
                Log.e(TAG, "Error patching EPUB", e)
                Result.failure(e)
            }
        }

        /**
         * Finds the path to the OPF file by reading META-INF/container.xml
         */
        private fun findOPFPath(epubFile: File): String? {
            return try {
                val containerXml = readFileFromZip(epubFile, "META-INF/container.xml")
                if (containerXml == null) {
                    Log.e(TAG, "container.xml not found")
                    return null
                }

                // Extract full-path attribute from rootfile element
                val fullPathRegex = """<rootfile[^>]+full-path="([^"]+)"""".toRegex()
                val match = fullPathRegex.find(containerXml)
                match?.groupValues?.get(1)
            } catch (e: Exception) {
                Log.e(TAG, "Error finding OPF path", e)
                null
            }
        }

        /**
         * Reads a file from a ZIP archive
         */
        private fun readFileFromZip(zipFile: File, entryPath: String): String? {
            return try {
                ZipFile(zipFile).use { zip ->
                    val entry = zip.getEntry(entryPath) ?: return null
                    zip.getInputStream(entry).bufferedReader().use { it.readText() }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading from ZIP: $entryPath", e)
                null
            }
        }

        /**
         * Patches OPF content based on EPUB version
         */
        private fun patchOPFContent(opfContent: String, cleanedTitle: String?): String {
            // Detect EPUB version
            val versionRegex = """<package[^>]+version="([^"]+)"""".toRegex()
            val versionMatch = versionRegex.find(opfContent)
            val version = versionMatch?.groupValues?.get(1) ?: "2.0"

            Log.d(TAG, "Detected EPUB version: $version")

            var patched = if (version.startsWith("2")) {
                patchEPUB2(opfContent)
            } else {
                patchEPUB3(opfContent)
            }

            // Apply title cleaning if requested
            if (cleanedTitle != null) {
                patched = cleanTitle(patched, cleanedTitle)
            }

            return patched
        }

        /**
         * Patches EPUB 2.0: Remove xmlns:calibre and fix attribute/tag order
         */
        private fun patchEPUB2(opfContent: String): String {
            var patched = opfContent

            // Step 1: Remove xmlns:calibre from <metadata> tag
            patched = removeCalibreNamespace(patched)

            // Step 2: Ensure calibre:series comes before calibre:series_index
            patched = fixSeriesTagOrder(patched)

            // Step 3: Ensure name attribute comes before content attribute in <meta> tags
            patched = fixMetaAttributeOrder(patched)

            return patched
        }

        /**
         * Patches EPUB 3.0: Add namespaces and opf: prefixes
         */
        private fun patchEPUB3(opfContent: String): String {
            var patched = opfContent

            // Step 1: Add namespace declarations to <metadata> tag if missing
            patched = ensureMetadataNamespaces(patched)

            // Step 2: Convert <meta property="..."> to <opf:meta property="...">
            // But leave <meta name="..."> unchanged
            patched = addOpfPrefixToMetaTags(patched)

            return patched
        }

        /**
         * Removes xmlns:calibre namespace declaration from <metadata> tag
         * This fixes a Pocketbook bug where calibre:series is ignored when namespace is declared
         */
        private fun removeCalibreNamespace(opfContent: String): String {
            val metadataRegex = """<metadata([^>]*)>""".toRegex()
            val match = metadataRegex.find(opfContent) ?: return opfContent

            val metadataTag = match.value
            val attributes = match.groupValues[1]

            // Remove xmlns:calibre="..." from attributes
            val calibreNsRegex = """\s*xmlns:calibre="[^"]*"""".toRegex()
            val newAttributes = attributes.replace(calibreNsRegex, "")

            if (newAttributes == attributes) {
                return opfContent // No xmlns:calibre found
            }

            val newMetadataTag = "<metadata$newAttributes>"
            Log.d(TAG, "Removed xmlns:calibre from <metadata> tag")
            
            return opfContent.replace(metadataTag, newMetadataTag)
        }

        /**
         * Ensures calibre:series comes before calibre:series_index
         */
        private fun fixSeriesTagOrder(opfContent: String): String {
            // Find both tags
            val seriesRegex = """(<meta\s+name="calibre:series"[^>]*>)""".toRegex()
            val seriesIndexRegex = """(<meta\s+name="calibre:series_index"[^>]*>)""".toRegex()

            val seriesMatch = seriesRegex.find(opfContent)
            val seriesIndexMatch = seriesIndexRegex.find(opfContent)

            // If both exist, check order
            if (seriesMatch != null && seriesIndexMatch != null) {
                val seriesPos = seriesMatch.range.first
                val seriesIndexPos = seriesIndexMatch.range.first

                if (seriesIndexPos < seriesPos) {
                    // Wrong order! series_index comes before series
                    // Remove both and re-add in correct order
                    val seriesTag = seriesMatch.value
                    val seriesIndexTag = seriesIndexMatch.value

                    var patched = opfContent.replaceFirst(seriesTag, "")
                    patched = patched.replaceFirst(seriesIndexTag, "")

                    // Find where to insert (after the last remaining calibre: meta tag, or at the position of the first removed tag)
                    val insertPos = minOf(seriesPos, seriesIndexPos)
                    
                    // Insert both in correct order
                    patched = patched.substring(0, insertPos) + 
                              seriesTag + "\n    " + seriesIndexTag + "\n    " +
                              patched.substring(insertPos)

                    Log.d(TAG, "Fixed series tag order: series now before series_index")
                    return patched
                }
            }

            return opfContent
        }

        /**
         * Ensures 'name' attribute comes before 'content' attribute in <meta> tags
         */
        private fun fixMetaAttributeOrder(opfContent: String): String {
            // Match <meta> tags with both name and content attributes where content comes first
            val wrongOrderRegex = """<meta(\s+)content="([^"]*)"(\s+)name="([^"]*)"([^>]*)>""".toRegex()
            
            val patched = wrongOrderRegex.replace(opfContent) { matchResult ->
                val space1 = matchResult.groupValues[1]
                val contentValue = matchResult.groupValues[2]
                val space2 = matchResult.groupValues[3]
                val nameValue = matchResult.groupValues[4]
                val rest = matchResult.groupValues[5]
                
                // Rebuild with correct order: name before content
                "<meta${space1}name=\"$nameValue\"${space2}content=\"$contentValue\"$rest>"
            }

            if (patched != opfContent) {
                Log.d(TAG, "Fixed attribute order in <meta> tags: name now before content")
            }

            return patched
        }

        /**
         * Ensures <metadata> tag has all required namespace declarations (EPUB 3.0)
         */
        private fun ensureMetadataNamespaces(opfContent: String): String {
            val metadataRegex = """<metadata([^>]*)>""".toRegex()
            val match = metadataRegex.find(opfContent) ?: return opfContent

            var metadataTag = match.value
            var attributes = match.groupValues[1]

            // Define required namespaces
            val requiredNamespaces = mapOf(
                "xmlns:opf" to "http://www.idpf.org/2007/opf",
                "xmlns:dc" to "http://purl.org/dc/elements/1.1/",
                "xmlns:dcterms" to "http://purl.org/dc/terms/",
                "xmlns:xsi" to "http://www.w3.org/2001/XMLSchema-instance",
                "xmlns:calibre" to "http://calibre.kovidgoyal.net/2009/metadata"
            )

            // Check which namespaces are missing
            val namespacesToAdd = mutableListOf<String>()
            for ((nsPrefix, nsUri) in requiredNamespaces) {
                if (!attributes.contains(nsPrefix)) {
                    namespacesToAdd.add("""$nsPrefix="$nsUri"""")
                }
            }

            if (namespacesToAdd.isEmpty()) {
                return opfContent // All namespaces already present
            }

            // Add missing namespaces
            val newAttributes = if (attributes.isBlank()) {
                " " + namespacesToAdd.joinToString(" ")
            } else {
                "$attributes " + namespacesToAdd.joinToString(" ")
            }

            val newMetadataTag = "<metadata$newAttributes>"
            
            return opfContent.replace(metadataTag, newMetadataTag)
        }

        /**
         * Adds opf: prefix to specific meta tags that need it for Pocketbook compatibility
         */
        private fun addOpfPrefixToMetaTags(opfContent: String): String {
            var patched = opfContent

            // Tags that need opf: prefix (based on successful manual patch):
            // 1. <meta property="belongs-to-collection">
            // 2. <meta refines="#..." property="collection-type">
            // 3. <meta refines="#..." property="group-position">
            // 4. <meta refines="#..." property="title-type">
            // 5. <meta refines="#..." property="file-as">
            // 6. <meta refines="#..." property="identifier-type">
            // 7. <meta refines="#..." property="role">
            
            // General rule: <meta> tags with 'property' OR 'refines' attributes need opf: prefix
            // Exception: <meta property="dcterms:..."> and <meta property="calibre:..."> stay as <meta>

            // Opening tags
            val metaPropertyRegex = """<meta(\s+[^>]*(?:property|refines)="[^"]*"[^>]*)>""".toRegex()
            patched = metaPropertyRegex.replace(patched) { matchResult ->
                val attributes = matchResult.groupValues[1]
                val fullMatch = matchResult.value
                
                // Skip if already has opf: prefix
                if (fullMatch.startsWith("<opf:meta")) {
                    return@replace fullMatch
                }
                
                // Skip if has 'name' attribute (these stay as <meta>)
                if (attributes.contains("""name=""")) {
                    return@replace fullMatch
                }
                
                // Skip if property starts with dcterms: or calibre: (based on the working example)
                if (attributes.contains("""property="dcterms:""") || 
                    attributes.contains("""property="calibre:""")) {
                    return@replace fullMatch
                }
                
                // Convert to opf:meta
                "<opf:meta$attributes>"
            }

            // Closing tags - need to match the opening tags we converted
            // Count how many <opf:meta> vs </opf:meta> we have
            val opfMetaOpenCount = """<opf:meta\s""".toRegex().findAll(patched).count()
            val opfMetaCloseCount = """</opf:meta>""".toRegex().findAll(patched).count()
            val plainMetaCloseCount = """</meta>""".toRegex().findAll(patched).count()
            
            // Replace the difference
            val toReplace = opfMetaOpenCount - opfMetaCloseCount
            if (toReplace > 0) {
                var replaced = 0
                patched = """</meta>""".toRegex().replace(patched) { matchResult ->
                    if (replaced < toReplace) {
                        replaced++
                        "</opf:meta>"
                    } else {
                        matchResult.value
                    }
                }
            }

            return patched
        }

        /**
         * Creates a new EPUB file with the patched OPF content
         */
        private fun createPatchedEpub(
            originalEpub: File,
            patchedEpub: File,
            opfPath: String,
            patchedOpfContent: String
        ) {
            ZipFile(originalEpub).use { zipIn ->
                ZipOutputStream(patchedEpub.outputStream().buffered()).use { zipOut ->
                    // Copy all entries, replacing the OPF file
                    val entries = zipIn.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val entryName = entry.name

                        // Create new entry with same name
                        zipOut.putNextEntry(ZipEntry(entryName))

                        if (entryName == opfPath) {
                            // Write patched OPF content
                            zipOut.write(patchedOpfContent.toByteArray(Charsets.UTF_8))
                        } else {
                            // Copy original content
                            zipIn.getInputStream(entry).use { input ->
                                input.copyTo(zipOut)
                            }
                        }

                        zipOut.closeEntry()
                    }
                }
            }
        }

        /**
         * Cleans titles in the EPUB based on the app's cleaned title
         * 
         * @param opfContent The OPF file content
         * @param cleanedTitle The cleaned title from the app (with article at end, e.g. "letzte Fähre, Die")
         */
        private fun cleanTitle(opfContent: String, cleanedTitle: String): String {
            Log.d(TAG, "Cleaning title using: $cleanedTitle")
            
            var patched = opfContent

            // Move article from end to front for display title
            val displayTitle = moveArticleToFront(cleanedTitle)
            Log.d(TAG, "Display title: $displayTitle")

            // 1. Update <dc:title> with article moved to front
            val dcTitleRegex = """<dc:title[^>]*>([^<]+)</dc:title>""".toRegex()
            patched = dcTitleRegex.replace(patched) { matchResult ->
                val openingTag = matchResult.value.substringBefore(">") + ">"
                val closingTag = "</dc:title>"
                "$openingTag$displayTitle$closingTag"
            }

            // 2. Update/add <meta name="calibre:title_sort"> with cleaned title (article at end)
            val titleSortRegex = """<meta\s+name="calibre:title_sort"\s+content="[^"]*"\s*/?>""".toRegex()
            if (titleSortRegex.containsMatchIn(patched)) {
                // Replace existing
                patched = titleSortRegex.replace(patched) {
                    """<meta name="calibre:title_sort" content="$cleanedTitle"/>"""
                }
            } else {
                // Add new (find a good place - after other calibre: meta tags or before </metadata>)
                val calibreSeriesRegex = """(<meta\s+name="calibre:series_index"[^>]*>)""".toRegex()
                val calibreMatch = calibreSeriesRegex.find(patched)
                if (calibreMatch != null) {
                    // Insert after calibre:series_index
                    val insertPos = calibreMatch.range.last + 1
                    patched = patched.substring(0, insertPos) + 
                              "\n    <meta name=\"calibre:title_sort\" content=\"$cleanedTitle\"/>" +
                              patched.substring(insertPos)
                } else {
                    // Insert before </metadata>
                    patched = patched.replace("</metadata>", 
                        "    <meta name=\"calibre:title_sort\" content=\"$cleanedTitle\"/>\n  </metadata>")
                }
            }

            // 3. Update <opf:meta refines="#..." property="file-as"> (EPUB 3.0)
            val fileAsRegex = """<opf:meta\s+refines="#opf_title"\s+property="file-as">([^<]+)</opf:meta>""".toRegex()
            patched = fileAsRegex.replace(patched) {
                """<opf:meta refines="#opf_title" property="file-as">$cleanedTitle</opf:meta>"""
            }

            // Alternative pattern without opf: prefix
            val fileAsRegex2 = """<meta\s+refines="#opf_title"\s+property="file-as">([^<]+)</meta>""".toRegex()
            patched = fileAsRegex2.replace(patched) {
                """<meta refines="#opf_title" property="file-as">$cleanedTitle</meta>"""
            }

            Log.d(TAG, "Title cleaning complete")
            return patched
        }

        /**
         * Moves article from end to front
         * "letzte Fähre, Die" → "Die letzte Fähre"
         * "Turm der Lichter, Der" → "Der Turm der Lichter"
         */
        private fun moveArticleToFront(title: String): String {
            val articlePattern = """,\s+(Der|Die|Das|Ein|Eine)$""".toRegex()
            val match = articlePattern.find(title)
            
            return if (match != null) {
                val article = match.groupValues[1]
                val restOfTitle = title.substring(0, match.range.first)
                "$article $restOfTitle"
            } else {
                title
            }
        }
    }
}

