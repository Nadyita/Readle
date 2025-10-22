package com.readle.app.util

import org.junit.Test
import org.junit.Assert.assertEquals

/**
 * Comprehensive tests for TextNormalizer to ensure no regressions in title and author
 * name conversions during Audiobookshelf imports.
 *
 * Run tests via: ./gradlew test --tests "com.readle.app.util.TextNormalizerTest"
 * Run all tests: ./gradlew test
 */
class TextNormalizerTest {

    // =================================================================
    // Title Normalization Tests
    // =================================================================

    @Test
    fun `normalizeTitle - German article Der`() {
        assertEquals("kleine Prinz, Der", TextNormalizer.normalizeTitle("Der kleine Prinz"))
    }

    @Test
    fun `normalizeTitle - German article Die`() {
        assertEquals(
            "Haarteppichknüpfer, Die",
            TextNormalizer.normalizeTitle("Die Haarteppichknüpfer")
        )
    }

    @Test
    fun `normalizeTitle - German article Das`() {
        assertEquals("Boot, Das", TextNormalizer.normalizeTitle("Das Boot"))
    }

    @Test
    fun `normalizeTitle - German article case insensitive`() {
        assertEquals("Test, der", TextNormalizer.normalizeTitle("der Test"))
        assertEquals("Test, die", TextNormalizer.normalizeTitle("die Test"))
        assertEquals("Test, das", TextNormalizer.normalizeTitle("das Test"))
    }

    @Test
    fun `normalizeTitle - English article The`() {
        assertEquals("Great Gatsby, The", TextNormalizer.normalizeTitle("The Great Gatsby"))
        assertEquals("Matrix, The", TextNormalizer.normalizeTitle("The Matrix"))
    }

    @Test
    fun `normalizeTitle - English article case insensitive`() {
        assertEquals("Test, The", TextNormalizer.normalizeTitle("the Test"))
        assertEquals("Test, The", TextNormalizer.normalizeTitle("THE Test"))
    }

    // BEISPIEL: So kannst du einen neuen Test hinzufügen
    @Test
    fun `normalizeTitle - CUSTOM TEST EXAMPLE - kannst du ändern oder löschen`() {
        // Beispiel 1: Deutscher Titel
        assertEquals("Zauberberg, Der", TextNormalizer.normalizeTitle("Der Zauberberg"))
        
        // Beispiel 2: Englischer Titel
        assertEquals("Lord of the Rings, The", TextNormalizer.normalizeTitle("The Lord of the Rings"))
        
        // Beispiel 3: Titel ohne Artikel (bleibt unverändert)
        assertEquals("Harry Potter", TextNormalizer.normalizeTitle("Harry Potter"))
    }

    @Test
    fun `normalizeTitle - French article Le`() {
        assertEquals("Petit Prince, Le", TextNormalizer.normalizeTitle("Le Petit Prince"))
    }

    @Test
    fun `normalizeTitle - French article La`() {
        assertEquals("Vie en Rose, La", TextNormalizer.normalizeTitle("La Vie en Rose"))
    }

    @Test
    fun `normalizeTitle - French article Les`() {
        assertEquals("Misérables, Les", TextNormalizer.normalizeTitle("Les Misérables"))
    }

    @Test
    fun `normalizeTitle - French article L apostrophe`() {
        assertEquals("Étranger, L'", TextNormalizer.normalizeTitle("L'Étranger"))
    }

    @Test
    fun `normalizeTitle - no article should remain unchanged`() {
        assertEquals("Harry Potter", TextNormalizer.normalizeTitle("Harry Potter"))
        assertEquals("1984", TextNormalizer.normalizeTitle("1984"))
        assertEquals("Herr der Ringe", TextNormalizer.normalizeTitle("Herr der Ringe"))
    }

    @Test
    fun `normalizeTitle - article in middle should not be moved`() {
        assertEquals("Lord of the Rings", TextNormalizer.normalizeTitle("Lord of the Rings"))
        assertEquals("Herr der Ringe", TextNormalizer.normalizeTitle("Herr der Ringe"))
    }

    @Test
    fun `normalizeTitle - whitespace handling`() {
        assertEquals("Test, Der", TextNormalizer.normalizeTitle("  Der Test  "))
        assertEquals("Test, The", TextNormalizer.normalizeTitle("  The Test  "))
    }

    @Test
    fun `normalizeTitle - single word with article`() {
        assertEquals("Prozess, Der", TextNormalizer.normalizeTitle("Der Prozess"))
    }

    @Test
    fun `normalizeTitle - empty string`() {
        assertEquals("", TextNormalizer.normalizeTitle(""))
    }

    @Test
    fun `normalizeTitle - only article`() {
        assertEquals("Der", TextNormalizer.normalizeTitle("Der"))
        assertEquals("The", TextNormalizer.normalizeTitle("The"))
    }

    @Test
    fun `normalizeTitle - special characters and numbers`() {
        assertEquals(
            "100-Jährige, Der",
            TextNormalizer.normalizeTitle("Der 100-Jährige")
        )
        assertEquals(
            "5th Wave, The",
            TextNormalizer.normalizeTitle("The 5th Wave")
        )
    }

    @Test
    fun `normalizeTitle - Unicode characters`() {
        assertEquals(
            "Nibelungen, Die",
            TextNormalizer.normalizeTitle("Die Nibelungen")
        )
        assertEquals(
            "Räuber, Die",
            TextNormalizer.normalizeTitle("Die Räuber")
        )
    }

    // =================================================================
    // Author Normalization Tests - Single Author
    // =================================================================

    @Test
    fun `normalizeAuthor - simple first and last name`() {
        assertEquals("Eschbach, Andreas", TextNormalizer.normalizeAuthor("Andreas Eschbach"))
        assertEquals("King, Stephen", TextNormalizer.normalizeAuthor("Stephen King"))
    }

    @Test
    fun `normalizeAuthor - author with initials`() {
        assertEquals("Rowling, J.K.", TextNormalizer.normalizeAuthor("J.K. Rowling"))
        assertEquals("Tolkien, J.R.R.", TextNormalizer.normalizeAuthor("J.R.R. Tolkien"))
    }

    @Test
    fun `normalizeAuthor - already in LastName FirstName format`() {
        assertEquals("Eschbach, Andreas", TextNormalizer.normalizeAuthor("Eschbach, Andreas"))
        assertEquals("Tolkien, J.R.R.", TextNormalizer.normalizeAuthor("Tolkien, J.R.R."))
    }

    @Test
    fun `normalizeAuthor - three part name`() {
        assertEquals(
            "Brown, Dan Marcus",
            TextNormalizer.normalizeAuthor("Dan Marcus Brown")
        )
        assertEquals(
            "Márquez, Gabriel García",
            TextNormalizer.normalizeAuthor("Gabriel García Márquez")
        )
    }

    @Test
    fun `normalizeAuthor - four part name`() {
        assertEquals(
            "Smith, John David Michael",
            TextNormalizer.normalizeAuthor("John David Michael Smith")
        )
    }

    @Test
    fun `normalizeAuthor - single name only`() {
        assertEquals("Madonna", TextNormalizer.normalizeAuthor("Madonna"))
        assertEquals("Voltaire", TextNormalizer.normalizeAuthor("Voltaire"))
    }

    @Test
    fun `normalizeAuthor - name with multiple spaces`() {
        assertEquals(
            "King, Stephen",
            TextNormalizer.normalizeAuthor("Stephen  King")
        )
        assertEquals(
            "Rowling, J.K.",
            TextNormalizer.normalizeAuthor("J.K.  Rowling")
        )
    }

    @Test
    fun `normalizeAuthor - whitespace handling`() {
        assertEquals(
            "Eschbach, Andreas",
            TextNormalizer.normalizeAuthor("  Andreas Eschbach  ")
        )
    }

    @Test
    fun `normalizeAuthor - empty string`() {
        assertEquals("", TextNormalizer.normalizeAuthor(""))
    }

    @Test
    fun `normalizeAuthor - name with hyphens`() {
        assertEquals(
            "Müller-Schmidt, Hans",
            TextNormalizer.normalizeAuthor("Hans Müller-Schmidt")
        )
    }

    @Test
    fun `normalizeAuthor - name with apostrophe`() {
        assertEquals(
            "O'Brien, Patrick",
            TextNormalizer.normalizeAuthor("Patrick O'Brien")
        )
    }

    // =================================================================
    // Author Normalization Tests - Multiple Authors
    // =================================================================

    @Test
    fun `normalizeAuthor - two authors separated by semicolon`() {
        assertEquals(
            "Doe, John; Smith, Jane",
            TextNormalizer.normalizeAuthor("John Doe; Jane Smith")
        )
    }

    @Test
    fun `normalizeAuthor - three authors separated by semicolon`() {
        assertEquals(
            "Doe, John; Smith, Jane; Brown, Bob",
            TextNormalizer.normalizeAuthor("John Doe; Jane Smith; Bob Brown")
        )
    }

    @Test
    fun `normalizeAuthor - multiple authors already normalized`() {
        assertEquals(
            "Doe, John; Smith, Jane",
            TextNormalizer.normalizeAuthor("Doe, John; Smith, Jane")
        )
    }

    @Test
    fun `normalizeAuthor - mixed format multiple authors`() {
        // First author normalized, second not - both should be normalized
        assertEquals(
            "Doe, John; Smith, Jane",
            TextNormalizer.normalizeAuthor("Doe, John; Jane Smith")
        )
    }

    @Test
    fun `normalizeAuthor - multiple authors with initials`() {
        assertEquals(
            "Tolkien, J.R.R.; Lewis, C.S.",
            TextNormalizer.normalizeAuthor("J.R.R. Tolkien; C.S. Lewis")
        )
    }

    @Test
    fun `normalizeAuthor - multiple authors with three-part names`() {
        assertEquals(
            "Márquez, Gabriel García; Llosa, Mario Vargas",
            TextNormalizer.normalizeAuthor("Gabriel García Márquez; Mario Vargas Llosa")
        )
    }

    @Test
    fun `normalizeAuthor - multiple authors with whitespace`() {
        assertEquals(
            "Doe, John; Smith, Jane",
            TextNormalizer.normalizeAuthor("  John Doe  ;  Jane Smith  ")
        )
    }

    @Test
    fun `normalizeAuthor - multiple authors one has single name`() {
        assertEquals(
            "King, Stephen; Madonna",
            TextNormalizer.normalizeAuthor("Stephen King; Madonna")
        )
    }

    // =================================================================
    // Edge Cases and Special Scenarios
    // =================================================================

    @Test
    fun `normalizeAuthor - German umlauts`() {
        assertEquals("Müller, Hans", TextNormalizer.normalizeAuthor("Hans Müller"))
        assertEquals("Schäfer, Klaus", TextNormalizer.normalizeAuthor("Klaus Schäfer"))
        assertEquals("Böhm, Ludwig", TextNormalizer.normalizeAuthor("Ludwig Böhm"))
    }

    @Test
    fun `normalizeAuthor - names with von, van, de prefixes`() {
        // Note: Current implementation treats "von", "van", "de" as part of first names
        // not as noble prefixes - this is acceptable for most use cases
        assertEquals(
            "Goethe, Johann Wolfgang von",
            TextNormalizer.normalizeAuthor("Johann Wolfgang von Goethe")
        )
        assertEquals(
            "Beethoven, Ludwig van",
            TextNormalizer.normalizeAuthor("Ludwig van Beethoven")
        )
    }

    @Test
    fun `normalizeTitle - title with special formatting`() {
        assertEquals(
            "Herr der Ringe: Die Gefährten, Der",
            TextNormalizer.normalizeTitle("Der Herr der Ringe: Die Gefährten")
        )
    }

    @Test
    fun `normalizeTitle - series with articles`() {
        assertEquals(
            "Stunde der Wächter, Die",
            TextNormalizer.normalizeTitle("Die Stunde der Wächter")
        )
    }

    // =================================================================
    // Real-world Examples from Audiobookshelf
    // =================================================================

    @Test
    fun `normalizeAuthor - real example Ina Linger and Doska Palifin`() {
        // After convertAuthorSeparator, this would be "Ina Linger; Doska Palifin"
        assertEquals(
            "Linger, Ina; Palifin, Doska",
            TextNormalizer.normalizeAuthor("Ina Linger; Doska Palifin")
        )
    }

    @Test
    fun `normalizeAuthor - real example already in LF format with semicolon`() {
        // After convertAuthorSeparator converts "Linger, Ina, Palifin, Doska"
        // to "Linger, Ina; Palifin, Doska"
        assertEquals(
            "Linger, Ina; Palifin, Doska",
            TextNormalizer.normalizeAuthor("Linger, Ina; Palifin, Doska")
        )
    }

    @Test
    fun `normalizeTitle - real example Die Haarteppichknüpfer`() {
        assertEquals(
            "Haarteppichknüpfer, Die",
            TextNormalizer.normalizeTitle("Die Haarteppichknüpfer")
        )
    }

    @Test
    fun `normalizeAuthor - complex hyphenated German name`() {
        assertEquals(
            "Goethe, Johann Wolfgang von",
            TextNormalizer.normalizeAuthor("Johann Wolfgang von Goethe")
        )
    }

    @Test
    fun `normalizeTitle - article Das with compound words`() {
        assertEquals(
            "Parfum: Die Geschichte eines Mörders, Das",
            TextNormalizer.normalizeTitle("Das Parfum: Die Geschichte eines Mörders")
        )
    }

    // =================================================================
    // Regression Prevention Tests
    // =================================================================

    @Test
    fun `normalizeTitle - regression no double comma`() {
        val result = TextNormalizer.normalizeTitle("Der Test")
        assertEquals("Test, Der", result)
        assertEquals(1, result.count { it == ',' })
    }

    @Test
    fun `normalizeAuthor - regression no double semicolon`() {
        val result = TextNormalizer.normalizeAuthor("John Doe; Jane Smith")
        assertEquals("Doe, John; Smith, Jane", result)
        assertEquals(1, result.count { it == ';' })
    }

    @Test
    fun `normalizeAuthor - regression preserve existing comma in LastName FirstName`() {
        val result = TextNormalizer.normalizeAuthor("Doe, John")
        assertEquals("Doe, John", result)
        assertEquals(1, result.count { it == ',' })
    }

    @Test
    fun `normalizeTitle - regression preserve original when no article`() {
        val original = "Harry Potter und der Stein der Weisen"
        val result = TextNormalizer.normalizeTitle(original)
        assertEquals(original, result)
    }

    @Test
    fun `normalizeAuthor - regression trim all parts`() {
        val result = TextNormalizer.normalizeAuthor("  John   Doe  ;  Jane   Smith  ")
        assertEquals("Doe, John; Smith, Jane", result)
        // Verify no extra spaces
        assertFalse(result.contains("  "))
    }

    private fun assertFalse(condition: Boolean) {
        assertEquals(false, condition)
    }
}

