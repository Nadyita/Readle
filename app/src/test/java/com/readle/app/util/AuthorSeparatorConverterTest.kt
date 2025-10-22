package com.readle.app.util

import org.junit.Test
import org.junit.Assert.assertEquals

/**
 * Comprehensive tests for Audiobookshelf author separator conversion logic.
 * Tests the AuthorSeparatorConverter utility to ensure no regressions in
 * author name format conversions during imports.
 *
 * This function handles two formats:
 * - Format 1 (authorName): "FirstName LastName, FirstName LastName"
 * - Format 2 (authorNameLF): "LastName, FirstName, LastName, FirstName"
 *
 * Run tests via: ./gradlew :app:testDebugUnitTest --tests "AuthorSeparatorConverterTest"
 * Run all unit tests: ./gradlew :app:testDebugUnitTest
 */
class AuthorSeparatorConverterTest {

    // =================================================================
    // Format 1 Tests: authorName (FirstName LastName, FirstName LastName)
    // isLastNameFirst = false
    // =================================================================

    @Test
    fun `Format 1 - single author no comma`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator("John Doe", false)
        assertEquals("John Doe", result)
    }

    @Test
    fun `Format 1 - two authors`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "Ina Linger, Doska Palifin",
            false
        )
        assertEquals("Ina Linger; Doska Palifin", result)
    }

    @Test
    fun `Format 1 - three authors`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "John Doe, Jane Smith, Bob Brown",
            false
        )
        assertEquals("John Doe; Jane Smith; Bob Brown", result)
    }

    @Test
    fun `Format 1 - four authors`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "Alice White, Bob Black, Charlie Green, Diana Blue",
            false
        )
        assertEquals("Alice White; Bob Black; Charlie Green; Diana Blue", result)
    }

    @Test
    fun `Format 1 - authors with initials`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "J.K. Rowling, J.R.R. Tolkien",
            false
        )
        assertEquals("J.K. Rowling; J.R.R. Tolkien", result)
    }

    @Test
    fun `Format 1 - authors with middle names`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "John David Smith, Mary Jane Doe",
            false
        )
        assertEquals("John David Smith; Mary Jane Doe", result)
    }

    @Test
    fun `Format 1 - German umlauts`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "Hans Müller, Klaus Schäfer",
            false
        )
        assertEquals("Hans Müller; Klaus Schäfer", result)
    }

    // =================================================================
    // Format 2 Tests: authorNameLF (LastName, FirstName, LastName, FirstName)
    // isLastNameFirst = true
    // =================================================================

    @Test
    fun `Format 2 - single author`() {
        // Single author with 1 comma should remain unchanged
        val result = AuthorSeparatorConverter.convertAuthorSeparator("Doe, John", true)
        assertEquals("Doe, John", result)
    }

    @Test
    fun `Format 2 - two authors`() {
        // "LastName, FirstName, LastName, FirstName" (3 commas)
        // Should replace 2nd comma with semicolon
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "Linger, Ina, Palifin, Doska",
            true
        )
        assertEquals("Linger, Ina; Palifin, Doska", result)
    }

    @Test
    fun `Format 2 - three authors`() {
        // "Last1, First1, Last2, First2, Last3, First3" (5 commas)
        // Should replace 2nd and 4th commas with semicolons
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "Doe, John, Smith, Jane, Brown, Bob",
            true
        )
        assertEquals("Doe, John; Smith, Jane; Brown, Bob", result)
    }

    @Test
    fun `Format 2 - four authors`() {
        // 7 commas total, replace 2nd, 4th, 6th
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "White, Alice, Black, Bob, Green, Charlie, Blue, Diana",
            true
        )
        assertEquals("White, Alice; Black, Bob; Green, Charlie; Blue, Diana", result)
    }

    @Test
    fun `Format 2 - authors with initials`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "Rowling, J.K., Tolkien, J.R.R.",
            true
        )
        assertEquals("Rowling, J.K.; Tolkien, J.R.R.", result)
    }

    @Test
    fun `Format 2 - authors with middle names`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "Smith, John David, Doe, Mary Jane",
            true
        )
        assertEquals("Smith, John David; Doe, Mary Jane", result)
    }

    @Test
    fun `Format 2 - German names`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "Müller, Hans, Schäfer, Klaus",
            true
        )
        assertEquals("Müller, Hans; Schäfer, Klaus", result)
    }

    @Test
    fun `Format 2 - five authors`() {
        // 9 commas, replace 2nd, 4th, 6th, 8th
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "A, One, B, Two, C, Three, D, Four, E, Five",
            true
        )
        assertEquals("A, One; B, Two; C, Three; D, Four; E, Five", result)
    }

    // =================================================================
    // Edge Cases and Special Scenarios
    // =================================================================

    @Test
    fun `empty string`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator("", false)
        assertEquals("", result)
    }

    @Test
    fun `single name without comma`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator("Madonna", false)
        assertEquals("Madonna", result)
    }

    @Test
    fun `Format 1 - single comma two parts`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator("John Doe, Jane Smith", false)
        assertEquals("John Doe; Jane Smith", result)
    }

    @Test
    fun `Format 2 - only commas no spaces after`() {
        // Test with comma but no space (edge case)
        val result = AuthorSeparatorConverter.convertAuthorSeparator("Doe,John,Smith,Jane", true)
        assertEquals("Doe,John;Smith,Jane", result)
    }

    @Test
    fun `Format 1 - whitespace variations`() {
        // Commas with different spacing - if no space after comma, nothing replaced
        val result = AuthorSeparatorConverter.convertAuthorSeparator("John Doe,Jane Smith", false)
        // Current implementation looks for ", " so "," alone won't match
        assertEquals("John Doe,Jane Smith", result)
    }

    @Test
    fun `Format 2 - names with hyphens`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "Müller-Schmidt, Hans, Weber-Meyer, Klaus",
            true
        )
        assertEquals("Müller-Schmidt, Hans; Weber-Meyer, Klaus", result)
    }

    @Test
    fun `Format 1 - names with apostrophes`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "Patrick O'Brien, Sean O'Casey",
            false
        )
        assertEquals("Patrick O'Brien; Sean O'Casey", result)
    }

    @Test
    fun `Format 2 - names with apostrophes`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "O'Brien, Patrick, O'Casey, Sean",
            true
        )
        assertEquals("O'Brien, Patrick; O'Casey, Sean", result)
    }

    // =================================================================
    // Real-world Audiobookshelf Examples
    // =================================================================

    @Test
    fun `real example from Audiobookshelf authorName`() {
        // This is what comes from authorName field
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "Ina Linger, Doska Palifin",
            false
        )
        assertEquals("Ina Linger; Doska Palifin", result)
    }

    @Test
    fun `real example from Audiobookshelf authorNameLF`() {
        // This is what comes from authorNameLF field
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "Linger, Ina, Palifin, Doska",
            true
        )
        assertEquals("Linger, Ina; Palifin, Doska", result)
    }

    @Test
    fun `German author real example`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "Eschbach, Andreas, Brandhorst, Andreas",
            true
        )
        assertEquals("Eschbach, Andreas; Brandhorst, Andreas", result)
    }

    // =================================================================
    // Regression Prevention Tests
    // =================================================================

    @Test
    fun `regression - no double semicolons Format 1`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "Author One, Author Two, Author Three",
            false
        )
        assertEquals("Author One; Author Two; Author Three", result)
        // Verify no double semicolons
        assertFalse(result.contains(";;"))
    }

    @Test
    fun `regression - no double semicolons Format 2`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "Last1, First1, Last2, First2, Last3, First3",
            true
        )
        assertEquals("Last1, First1; Last2, First2; Last3, First3", result)
        // Verify no double semicolons
        assertFalse(result.contains(";;"))
    }

    @Test
    fun `regression - Format 2 preserves internal commas`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "Doe, John, Smith, Jane",
            true
        )
        // Each author should still have their internal comma
        assertEquals("Doe, John; Smith, Jane", result)
        assertEquals(2, result.count { it == ',' })
        assertEquals(1, result.count { it == ';' })
    }

    @Test
    fun `regression - Format 1 replaces all commas`() {
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "Author One, Author Two, Author Three, Author Four",
            false
        )
        // All commas should be replaced with semicolons
        assertEquals(0, result.count { it == ',' })
        assertEquals(3, result.count { it == ';' })
    }

    @Test
    fun `regression - Format 2 even comma count logic`() {
        // 5 commas (odd number) - should replace 2nd and 4th
        val result = AuthorSeparatorConverter.convertAuthorSeparator(
            "A, B, C, D, E, F",
            true
        )
        assertEquals("A, B; C, D; E, F", result)
        assertEquals(3, result.count { it == ',' })
        assertEquals(2, result.count { it == ';' })
    }

    @Test
    fun `regression - Format 2 single author unchanged`() {
        val input = "Eschbach, Andreas"
        val result = AuthorSeparatorConverter.convertAuthorSeparator(input, true)
        // Single author with one comma should remain unchanged
        assertEquals(input, result)
    }

    private fun assertFalse(condition: Boolean) {
        assertEquals(false, condition)
    }
}

