package com.readle.app.domain.util

import android.util.Log
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(MockitoJUnitRunner::class)
class EpubMetadataPatcherTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var logMock: MockedStatic<Log>

    @Before
    fun setUp() {
        // Mock all Android Log methods to prevent "Method not mocked" errors
        logMock = Mockito.mockStatic(Log::class.java)
        logMock.`when`<Int> { Log.d(Mockito.anyString(), Mockito.anyString()) }.thenReturn(0)
        logMock.`when`<Int> { Log.e(Mockito.anyString(), Mockito.anyString()) }.thenReturn(0)
        logMock.`when`<Int> { Log.w(Mockito.anyString(), Mockito.anyString()) }.thenReturn(0)
        logMock.`when`<Int> { Log.i(Mockito.anyString(), Mockito.anyString()) }.thenReturn(0)
    }

    @org.junit.After
    fun tearDown() {
        logMock.close()
    }

    @Test
    fun `test EPUB 3 patching with dcterms modified tag`() {
        // This test reproduces the bug where dcterms:modified tag gets mismatched closing tag
        val opfContent = """<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uuid_id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title id="opf_title">Test Book</dc:title>
    <meta property="dcterms:modified">2025-09-16T19:34:31Z</meta>
    <meta property="belongs-to-collection" id="collection_1">Test Series</meta>
    <meta refines="#collection_1" property="collection-type">series</meta>
    <meta refines="#collection_1" property="group-position">1</meta>
  </metadata>
  <manifest>
    <item id="ncx" href="toc.ncx" media-type="application/x-dtd-ncx"/>
  </manifest>
  <spine toc="ncx">
  </spine>
</package>"""

        val patchedContent = patchOPFContent(opfContent, null)

        // Verify that dcterms:modified tag has matching opening and closing tags
        assertTrue(
            "dcterms:modified should remain as <meta>...</meta>",
            patchedContent.contains("""<meta property="dcterms:modified">2025-09-16T19:34:31Z</meta>""")
        )

        // Verify that other meta tags are converted to opf:meta
        assertTrue(
            "belongs-to-collection should be converted to <opf:meta>",
            patchedContent.contains("""<opf:meta property="belongs-to-collection"""")
        )
        assertTrue(
            "belongs-to-collection closing tag should be </opf:meta>",
            patchedContent.contains("""Test Series</opf:meta>""")
        )

        // Make sure there are no mismatched tags
        assertFalse(
            "Should not have <meta> with </opf:meta> closing tag",
            patchedContent.contains("""<meta property="dcterms:modified">2025-09-16T19:34:31Z</opf:meta>""")
        )
    }

    @Test
    fun `test EPUB 3 patching with calibre metadata`() {
        val opfContent = """<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uuid_id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>Test Book</dc:title>
    <meta property="calibre:series">Test Series</meta>
    <meta property="calibre:series_index">1</meta>
    <meta property="belongs-to-collection" id="collection_1">Test Series</meta>
  </metadata>
  <manifest>
  </manifest>
  <spine>
  </spine>
</package>"""

        val patchedContent = patchOPFContent(opfContent, null)

        // calibre: meta tags should remain as <meta>
        assertTrue(
            "calibre:series should remain as <meta>",
            patchedContent.contains("""<meta property="calibre:series">Test Series</meta>""")
        )
        assertTrue(
            "calibre:series_index should remain as <meta>",
            patchedContent.contains("""<meta property="calibre:series_index">1</meta>""")
        )

        // Non-calibre/non-dcterms should be converted to opf:meta
        assertTrue(
            "belongs-to-collection should be converted to opf:meta",
            patchedContent.contains("""<opf:meta property="belongs-to-collection"""")
        )
    }

    @Test
    fun `test EPUB 3 patching with meta name tags`() {
        val opfContent = """<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uuid_id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>Test Book</dc:title>
    <meta name="cover" content="cover-image"/>
    <meta property="belongs-to-collection" id="collection_1">Test Series</meta>
  </metadata>
  <manifest>
  </manifest>
  <spine>
  </spine>
</package>"""

        val patchedContent = patchOPFContent(opfContent, null)

        // Meta tags with 'name' attribute should remain as <meta>
        assertTrue(
            "meta with name attribute should remain as <meta>",
            patchedContent.contains("""<meta name="cover" content="cover-image"/>""")
        )

        // Meta tags with 'property' should be converted
        assertTrue(
            "meta with property should be converted to opf:meta",
            patchedContent.contains("""<opf:meta property="belongs-to-collection"""")
        )
    }

    @Test
    fun `test EPUB 2 calibre namespace removal`() {
        val opfContent = """<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="uuid_id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:calibre="http://calibre.kovidgoyal.net/2009/metadata">
    <dc:title>Test Book</dc:title>
    <meta name="calibre:series" content="Test Series"/>
    <meta name="calibre:series_index" content="1"/>
  </metadata>
  <manifest>
  </manifest>
  <spine>
  </spine>
</package>"""

        val patchedContent = patchOPFContent(opfContent, null)

        // xmlns:calibre should be removed from metadata tag
        assertFalse(
            "xmlns:calibre should be removed from metadata",
            patchedContent.contains("""xmlns:calibre="http://calibre.kovidgoyal.net/2009/metadata"""")
        )

        // But calibre:series tags should still be there
        assertTrue(
            "calibre:series should still exist",
            patchedContent.contains("""calibre:series""")
        )
    }

    @Test
    fun `test complex EPUB 3 with mixed meta tags`() {
        val opfContent = """<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uuid_id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title id="opf_title">Test Book</dc:title>
    <dc:creator id="creator_1">John Doe</dc:creator>
    <meta name="cover" content="cover-image"/>
    <meta property="dcterms:modified">2025-09-16T19:34:31Z</meta>
    <meta property="calibre:series">Test Series</meta>
    <meta property="calibre:series_index">1</meta>
    <meta property="belongs-to-collection" id="collection_1">Test Series</meta>
    <meta refines="#collection_1" property="collection-type">series</meta>
    <meta refines="#collection_1" property="group-position">1</meta>
    <meta refines="#opf_title" property="title-type">main</meta>
    <meta refines="#opf_title" property="file-as">Test Book</meta>
    <meta refines="#creator_1" property="role" scheme="marc:relators">aut</meta>
    <meta refines="#creator_1" property="file-as">Doe, John</meta>
  </metadata>
  <manifest>
  </manifest>
  <spine>
  </spine>
</package>"""

        val patchedContent = patchOPFContent(opfContent, null)

        // Verify meta with 'name' attribute stays as <meta>
        assertTrue(
            patchedContent.contains("""<meta name="cover" content="cover-image"/>""")
        )

        // Verify dcterms: stays as <meta>
        assertTrue(
            patchedContent.contains("""<meta property="dcterms:modified">2025-09-16T19:34:31Z</meta>""")
        )

        // Verify calibre: stays as <meta>
        assertTrue(
            patchedContent.contains("""<meta property="calibre:series">Test Series</meta>""")
        )
        assertTrue(
            patchedContent.contains("""<meta property="calibre:series_index">1</meta>""")
        )

        // Verify belongs-to-collection is converted to opf:meta
        assertTrue(
            patchedContent.contains("""<opf:meta property="belongs-to-collection" id="collection_1">Test Series</opf:meta>""")
        )

        // Verify refines tags are converted to opf:meta
        assertTrue(
            patchedContent.contains("""<opf:meta refines="#collection_1" property="collection-type">series</opf:meta>""")
        )
        assertTrue(
            patchedContent.contains("""<opf:meta refines="#collection_1" property="group-position">1</opf:meta>""")
        )
        assertTrue(
            patchedContent.contains("""<opf:meta refines="#opf_title" property="title-type">main</opf:meta>""")
        )
        assertTrue(
            patchedContent.contains("""<opf:meta refines="#opf_title" property="file-as">Test Book</opf:meta>""")
        )
        assertTrue(
            patchedContent.contains("""<opf:meta refines="#creator_1" property="role" scheme="marc:relators">aut</opf:meta>""")
        )
        assertTrue(
            patchedContent.contains("""<opf:meta refines="#creator_1" property="file-as">Doe, John</opf:meta>""")
        )
    }

    @Test
    fun `test already patched EPUB is not modified`() {
        // Test that an already patched EPUB (with opf:meta tags) is not changed
        // (except for namespace declarations which may be added)
        val opfContent = """<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uuid_id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:calibre="http://calibre.kovidgoyal.net/2009/metadata">
    <dc:title id="opf_title">Test Book</dc:title>
    <meta property="dcterms:modified">2025-09-16T19:34:31Z</meta>
    <meta property="calibre:series">Test Series</meta>
    <opf:meta property="belongs-to-collection" id="collection_1">Test Series</opf:meta>
    <opf:meta refines="#collection_1" property="collection-type">series</opf:meta>
    <opf:meta refines="#collection_1" property="group-position">1</opf:meta>
  </metadata>
  <manifest>
  </manifest>
  <spine>
  </spine>
</package>"""

        val patchedContent = patchOPFContent(opfContent, null)

        // Verify that opf:meta tags remain unchanged
        assertTrue(
            "opf:meta tags should remain unchanged",
            patchedContent.contains("""<opf:meta property="belongs-to-collection" id="collection_1">Test Series</opf:meta>""")
        )
        assertTrue(
            "opf:meta closing tags should remain unchanged",
            patchedContent.contains("""</opf:meta>""")
        )
        
        // Verify that dcterms: and calibre: meta tags remain as <meta>
        assertTrue(
            "dcterms:modified should remain as <meta>",
            patchedContent.contains("""<meta property="dcterms:modified">2025-09-16T19:34:31Z</meta>""")
        )
        assertTrue(
            "calibre:series should remain as <meta>",
            patchedContent.contains("""<meta property="calibre:series">Test Series</meta>""")
        )
    }

    private fun patchOPFContent(opfContent: String, cleanedTitle: String?): String {
        return EpubMetadataPatcher.patchOPFContent(opfContent, cleanedTitle)
    }
}

