package com.readle.app.util

import org.junit.Test
import org.junit.Assert.assertEquals

/**
 * Comprehensive tests for TitleSeriesCleanup utility.
 * Tests the cleanup of book titles by removing redundant series names and numbers
 * from Audiobookshelf imports.
 *
 * This covers all scenarios:
 * - Series with German/English articles
 * - Series without articles
 * - Different numbering formats (#7, Band 7, 7.5, etc.)
 * - Omnibus/Collections (e.g., "Band 1-3")
 * - Book #1 special cases
 * - Lowercase titles (keep series name)
 * - Series with suffixes (-Saga, -Reihe, etc.)
 *
 * Run tests via: ./gradlew :app:testDebugUnitTest --tests "TitleSeriesCleanupTest"
 */
class TitleSeriesCleanupTest {

    // =================================================================
    // Series WITH Articles (Der, Die, Das, The)
    // =================================================================

    @Test
    fun `cleanup - series with Der - standard format`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Donnerstagsmordclub 5: Die letzte Teufelsnummer, Der",
            series = "Der Donnerstagsmordclub",
            seriesNumber = "5"
        )
        assertEquals("letzte Teufelsnummer, Die", result)
    }

    @Test
    fun `cleanup - series with Die - standard format`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Seiten der Welt 2: Blutbuch, Die",
            series = "Die Seiten der Welt",
            seriesNumber = "2"
        )
        assertEquals("Blutbuch", result)
    }

    @Test
    fun `cleanup - series with Das - standard format`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Lied von Eis und Feuer 1: Game of Thrones, Das",
            series = "Das Lied von Eis und Feuer",
            seriesNumber = "1"
        )
        assertEquals("Game of Thrones", result)
    }

    @Test
    fun `cleanup - series with The - standard format`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Hunger Games #1 - The Tributes",
            series = "The Hunger Games",
            seriesNumber = "1"
        )
        assertEquals("Tributes, The", result)
    }

    @Test
    fun `cleanup - series with Die - hash format`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Kristallwächter-Saga #7 - Der Fluss des Vergessens, Die",
            series = "Die Kristallwächter-Saga",
            seriesNumber = "7"
        )
        assertEquals("Fluss des Vergessens, Der", result)
    }

    @Test
    fun `cleanup - series with Die - colon separator`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Tribute von Panem 2: Gefährliche Liebe, Die",
            series = "Die Tribute von Panem",
            seriesNumber = "2"
        )
        // Title keeps original casing from rawTitle
        assertEquals("Gefährliche Liebe", result)
    }

    @Test
    fun `cleanup - series with Die - dash separator`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Chroniken von Narnia 1 - Der König von Narnia, Die",
            series = "Die Chroniken von Narnia",
            seriesNumber = "1"
        )
        assertEquals("König von Narnia, Der", result)
    }

    // =================================================================
    // Series WITHOUT Articles
    // =================================================================

    @Test
    fun `cleanup - series without article - standard format`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Foundation 1 - Foundation",
            series = "Foundation",
            seriesNumber = "1"
        )
        assertEquals("Foundation", result)
    }

    @Test
    fun `cleanup - series without article - with subtitle`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Alea Aquarius #7 - Der Fluss des Vergessens",
            series = "Alea Aquarius",
            seriesNumber = "7"
        )
        assertEquals("Fluss des Vergessens, Der", result)
    }

    @Test
    fun `cleanup - series without article - hash format`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Harry Potter #1 - Der Stein der Weisen",
            series = "Harry Potter",
            seriesNumber = "1"
        )
        assertEquals("Stein der Weisen, Der", result)
    }

    @Test
    fun `cleanup - series without article - colon separator`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Eragon 2: Der Auftrag des Ältesten",
            series = "Eragon",
            seriesNumber = "2"
        )
        assertEquals("Auftrag des Ältesten, Der", result)
    }

    @Test
    fun `cleanup - series without article - leading zero`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Foundation 01: Foundation",
            series = "Foundation",
            seriesNumber = "1"
        )
        assertEquals("Foundation", result)
    }

    // =================================================================
    // Series with Suffixes (-Saga, -Reihe, -Anthologie, etc.)
    // =================================================================

    @Test
    fun `cleanup - series with Saga suffix`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Kristallwächter #7 - Der Fluss des Vergessens",
            series = "Die Kristallwächter-Saga",
            seriesNumber = "7"
        )
        assertEquals("Fluss des Vergessens, Der", result)
    }

    @Test
    fun `cleanup - series with Reihe suffix`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Zeitreisende 3 - Die dritte Mission",
            series = "Zeitreisende-Reihe",
            seriesNumber = "3"
        )
        assertEquals("dritte Mission, Die", result)
    }

    @Test
    fun `cleanup - series with Anthologie suffix`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "SciFi 5: Zukunftsvisionen",
            series = "SciFi-Anthologie",
            seriesNumber = "5"
        )
        assertEquals("Zukunftsvisionen", result)
    }

    @Test
    fun `cleanup - series with Series suffix`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Mystery 2 - Dark Secrets",
            series = "Mystery-Series",
            seriesNumber = "2"
        )
        assertEquals("Dark Secrets", result)
    }

    // =================================================================
    // Decimal Numbers (e.g., 7.5)
    // =================================================================

    @Test
    fun `cleanup - decimal number with letter suffix a`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Foundation 7a - Foundation's Edge",
            series = "Foundation",
            seriesNumber = "7.5"
        )
        assertEquals("Foundation's Edge", result)
    }

    @Test
    fun `cleanup - decimal number with letter suffix b`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Scheibenwelt 07b - Das Licht der Fantasie",
            series = "Scheibenwelt",
            seriesNumber = "7.5"
        )
        assertEquals("Licht der Fantasie, Das", result)
    }

    @Test
    fun `cleanup - decimal number standard format`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Narnia 2.5: Das Pferd und sein Junge",
            series = "Narnia",
            seriesNumber = "2.5"
        )
        // Falls der Titel das decimal format direkt hat, sollte es funktionieren
        // Fallback auf letter suffix sollte greifen
        assertEquals("Pferd und sein Junge, Das", result)
    }

    // =================================================================
    // Book #1 Special Cases
    // =================================================================

    @Test
    fun `cleanup - book 1 without subtitle keeps series name`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Foundation 1",
            series = "Foundation",
            seriesNumber = "1"
        )
        assertEquals("Foundation", result)
    }

    @Test
    fun `cleanup - book 1 with article without subtitle`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Seiten der Welt 1, Die",
            series = "Die Seiten der Welt",
            seriesNumber = "1"
        )
        assertEquals("Seiten der Welt, Die", result)
    }

    @Test
    fun `cleanup - book 1 with leading zero`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Harry Potter 01",
            series = "Harry Potter",
            seriesNumber = "1"
        )
        assertEquals("Harry Potter", result)
    }

    @Test
    fun `cleanup - book 1 with subtitle gets normal treatment`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Foundation 1 - Foundation",
            series = "Foundation",
            seriesNumber = "1"
        )
        assertEquals("Foundation", result)
    }

    // =================================================================
    // Omnibus/Collections (Keep Intact)
    // =================================================================

    @Test
    fun `cleanup - omnibus 1-3 keeps full title`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Foundation 1-3",
            series = "Foundation",
            seriesNumber = "1"
        )
        assertEquals("Foundation 1-3", result)
    }

    @Test
    fun `cleanup - omnibus with article keeps full title with article`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Seiten der Welt 1-3, Die",
            series = "Die Seiten der Welt",
            seriesNumber = "1"
        )
        // TODO: Fix omnibus detection with article - currently returns "3"
        // Expected: "Seiten der Welt 1-3" or "Seiten der Welt 1-3, Die"
        // This test documents current behavior - needs fixing in TitleSeriesCleanup
        assertEquals("Seiten der Welt 1-3, Die", result)
    }

    @Test
    fun `cleanup - omnibus 1-4 keeps full title`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Harry Potter 1-4",
            series = "Harry Potter",
            seriesNumber = "1"
        )
        assertEquals("Harry Potter 1-4", result)
    }

    @Test
    fun `cleanup - omnibus 1-7 with article`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Chroniken von Narnia 1-7, Die",
            series = "Die Chroniken von Narnia",
            seriesNumber = "1"
        )
        // TODO: Fix omnibus detection with article - currently returns "7"
        // Expected: "Chroniken von Narnia 1-7, Die"
        // This test documents current behavior - needs fixing
        assertEquals("Chroniken von Narnia 1-7, Die", result)
    }

    // =================================================================
    // Lowercase Titles (Keep Series Name)
    // =================================================================

    @Test
    fun `cleanup - lowercase title keeps series name with article`() {
        // When extracted title starts with lowercase, keep series name
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Donnerstagsmordclub 5: und die gestohlene Katze, Der",
            series = "Der Donnerstagsmordclub",
            seriesNumber = "5"
        )
        // Should keep "Donnerstagsmordclub" and only remove number
        assertEquals("Donnerstagsmordclub und die gestohlene Katze, Der", result)
    }

    @Test
    fun `cleanup - lowercase title keeps series name without article`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Foundation 3 - and Empire",
            series = "Foundation",
            seriesNumber = "3"
        )
        // Should keep "Foundation" and only remove number
        assertEquals("Foundation and Empire", result)
    }

    // =================================================================
    // No Series or Number (Just Normalize)
    // =================================================================

    @Test
    fun `cleanup - no series info just normalizes title`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Der Herr der Ringe",
            series = null,
            seriesNumber = null
        )
        assertEquals("Herr der Ringe, Der", result)
    }

    @Test
    fun `cleanup - no series number just normalizes title`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "The Great Gatsby",
            series = "Some Series",
            seriesNumber = null
        )
        assertEquals("Great Gatsby, The", result)
    }

    @Test
    fun `cleanup - no series name just normalizes title`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Das Boot",
            series = null,
            seriesNumber = "1"
        )
        assertEquals("Boot, Das", result)
    }

    // =================================================================
    // Real-World Examples from Audiobookshelf
    // =================================================================

    @Test
    fun `real world - Alea Aquarius Band 7`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Alea Aquarius #7 - Der Fluss des Vergessens",
            series = "Alea Aquarius",
            seriesNumber = "7"
        )
        assertEquals("Fluss des Vergessens, Der", result)
    }

    @Test
    fun `real world - Der Donnerstagsmordclub Band 5`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Donnerstagsmordclub 5: Die letzte Teufelsnummer, Der",
            series = "Der Donnerstagsmordclub",
            seriesNumber = "5"
        )
        assertEquals("letzte Teufelsnummer, Die", result)
    }

    @Test
    fun `real world - Die Seiten der Welt Band 2`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Seiten der Welt 2: Blutbuch, Die",
            series = "Die Seiten der Welt",
            seriesNumber = "2"
        )
        assertEquals("Blutbuch", result)
    }

    @Test
    fun `real world - Foundation Band 1`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Foundation 1 - Foundation",
            series = "Foundation",
            seriesNumber = "1"
        )
        assertEquals("Foundation", result)
    }

    @Test
    fun `real world - Die Kristallwächter-Saga Band 7`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Kristallwächter-Saga #7 - Der Fluss des Vergessens, Die",
            series = "Die Kristallwächter-Saga",
            seriesNumber = "7"
        )
        assertEquals("Fluss des Vergessens, Der", result)
    }

    @Test
    fun `real world - Harry Potter Band 1`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Harry Potter #1 - Der Stein der Weisen",
            series = "Harry Potter",
            seriesNumber = "1"
        )
        assertEquals("Stein der Weisen, Der", result)
    }

    // =================================================================
    // Edge Cases
    // =================================================================

    @Test
    fun `edge case - series name equals title after cleanup`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Foundation 1 - Foundation",
            series = "Foundation",
            seriesNumber = "1"
        )
        // Should extract "Foundation" and normalize it
        assertEquals("Foundation", result)
    }

    @Test
    fun `edge case - very short title`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "HP 1 - A",
            series = "HP",
            seriesNumber = "1"
        )
        assertEquals("A", result)
    }

    @Test
    fun `edge case - title with special characters`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Serie #5 - Titel (mit Klammern) & Sonderzeichen!",
            series = "Serie",
            seriesNumber = "5"
        )
        assertEquals("Titel (mit Klammern) & Sonderzeichen!", result)
    }

    @Test
    fun `edge case - title without separator after number`() {
        // If there's no separator (-, :), the title should remain unchanged
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Foundation 1",
            series = "Foundation",
            seriesNumber = "1"
        )
        // Falls Buch #1 ohne Untertitel
        assertEquals("Foundation", result)
    }

    @Test
    fun `edge case - empty strings`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "",
            series = "",
            seriesNumber = ""
        )
        assertEquals("", result)
    }

    // =================================================================
    // Regression Prevention Tests
    // =================================================================

    @Test
    fun `regression - dont remove series if no match`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Totally Different Title",
            series = "Foundation",
            seriesNumber = "1"
        )
        // Series name not in title, so just normalize
        assertEquals("Totally Different Title", result)
    }

    @Test
    fun `regression - preserve article position after cleanup`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Seiten der Welt 2: Blutbuch, Die",
            series = "Die Seiten der Welt",
            seriesNumber = "2"
        )
        // Article is removed, title keeps its casing
        assertEquals("Blutbuch", result)
    }

    @Test
    fun `regression - omnibus detection must come before normal pattern`() {
        val result = TitleSeriesCleanup.cleanupTitle(
            rawTitle = "Foundation 1-3",
            series = "Foundation",
            seriesNumber = "1"
        )
        // Must NOT extract "3" as title, must keep full "Foundation 1-3"
        assertEquals("Foundation 1-3", result)
    }
}

