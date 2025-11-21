package com.readle.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerManualImportTest {
    
    @Test
    fun `cleanupManualImportTitle removes standard quotes`() {
        assertEquals(
            "Der Schwarm",
            TextNormalizer.cleanupManualImportTitle("\"Der Schwarm\"")
        )
    }
    
    @Test
    fun `cleanupManualImportTitle removes curly quotes`() {
        assertEquals(
            "Die Haarteppichknüpfer",
            TextNormalizer.cleanupManualImportTitle("\u201CDie Haarteppichknüpfer\u201D")
        )
    }
    
    @Test
    fun `cleanupManualImportTitle removes German quotes`() {
        assertEquals(
            "Der kleine Prinz",
            TextNormalizer.cleanupManualImportTitle("\u201EDer kleine Prinz\u201D")
        )
    }
    
    @Test
    fun `cleanupManualImportTitle removes guillemets`() {
        assertEquals(
            "Les Misérables",
            TextNormalizer.cleanupManualImportTitle("\u00ABLes Misérables\u00BB")
        )
    }
    
    @Test
    fun `cleanupManualImportTitle does not remove quotes if not surrounding entire title`() {
        assertEquals(
            "Der \"große\" Schwarm",
            TextNormalizer.cleanupManualImportTitle("Der \"große\" Schwarm")
        )
    }
    
    @Test
    fun `cleanupManualImportTitle removes colon Roman suffix`() {
        assertEquals(
            "Die Haarteppichknüpfer",
            TextNormalizer.cleanupManualImportTitle("Die Haarteppichknüpfer: Roman")
        )
    }
    
    @Test
    fun `cleanupManualImportTitle removes colon Roman suffix case insensitive`() {
        assertEquals(
            "Der Schwarm",
            TextNormalizer.cleanupManualImportTitle("Der Schwarm: roman")
        )
        assertEquals(
            "Der Schwarm",
            TextNormalizer.cleanupManualImportTitle("Der Schwarm: ROMAN")
        )
    }
    
    @Test
    fun `cleanupManualImportTitle removes both quotes and Roman suffix`() {
        assertEquals(
            "Die Haarteppichknüpfer",
            TextNormalizer.cleanupManualImportTitle("\"Die Haarteppichknüpfer: Roman\"")
        )
    }
    
    @Test
    fun `cleanupManualImportTitle handles quotes followed by Roman outside quotes`() {
        // Edge case: quotes around part of title, Roman after
        assertEquals(
            "\"Der kleine Prinz\"",
            TextNormalizer.cleanupManualImportTitle("\"Der kleine Prinz\": Roman")
        )
    }
    
    @Test
    fun `cleanupManualImportTitle preserves title with colon that is not Roman`() {
        assertEquals(
            "Harry Potter: Der Stein der Weisen",
            TextNormalizer.cleanupManualImportTitle("Harry Potter: Der Stein der Weisen")
        )
    }
    
    @Test
    fun `cleanupManualImportTitle handles empty string`() {
        assertEquals(
            "",
            TextNormalizer.cleanupManualImportTitle("")
        )
    }
    
    @Test
    fun `cleanupManualImportTitle handles title with only quotes`() {
        // Quotes of length 2 are not removed (length > 2 check in the function)
        val result = TextNormalizer.cleanupManualImportTitle("\"\"")
        assertEquals("\"\"", result)
    }
    
    @Test
    fun `cleanupManualImportTitle trims whitespace after cleanup`() {
        assertEquals(
            "Der Schwarm",
            TextNormalizer.cleanupManualImportTitle("  \"Der Schwarm: Roman\"  ")
        )
    }
    
    @Test
    fun `integration test - full cleanup and normalization flow`() {
        // Test the full flow: cleanup -> normalize -> titleSort
        val original = "\"Die Haarteppichknüpfer: Roman\""
        val cleaned = TextNormalizer.cleanupManualImportTitle(original)
        assertEquals("Die Haarteppichknüpfer", cleaned)
        
        val normalized = TextNormalizer.normalizeTitle(cleaned)
        assertEquals("Haarteppichknüpfer, Die", normalized)
        
        val titleSort = TextNormalizer.normalizeTitleForSorting(normalized, "de")
        // After normalization, the title is "Haarteppichknüpfer, Die"
        // normalizeTitleForSorting will not remove ", Die" because it's at the end
        // So we expect it to just remove leading "Die " if present
        assertEquals("Haarteppichknüpfer, Die", titleSort)
    }
    
    @Test
    fun `integration test - quoted title without article`() {
        val original = "\"Limit: Roman\""
        val cleaned = TextNormalizer.cleanupManualImportTitle(original)
        assertEquals("Limit", cleaned)
        
        val normalized = TextNormalizer.normalizeTitle(cleaned)
        assertEquals("Limit", normalized) // No article to move
    }
    
    @Test
    fun `integration test - title with Roman but no quotes`() {
        val original = "Der Schwarm: Roman"
        val cleaned = TextNormalizer.cleanupManualImportTitle(original)
        assertEquals("Der Schwarm", cleaned)
        
        val normalized = TextNormalizer.normalizeTitle(cleaned)
        assertEquals("Schwarm, Der", normalized)
    }
}

