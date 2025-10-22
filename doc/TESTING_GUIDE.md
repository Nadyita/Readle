# Testing Guide

This document describes how to run the test suite for the Readle book management application.

## Overview

The test suite covers multiple areas of the application:

1. **Unit Tests** - Testing individual components in isolation
2. **Integration Tests** - Testing database operations and API interactions
3. **UI Tests** - Testing Compose UI components

## Running Tests

### Run All Tests

```bash
./gradlew :app:testDebugUnitTest
```

### Run Specific Test Suite

```bash
# Text normalization tests
./gradlew :app:testDebugUnitTest --tests "com.readle.app.util.TextNormalizerTest"

# Author separator conversion tests
./gradlew :app:testDebugUnitTest --tests "com.readle.app.util.AuthorSeparatorConverterTest"

# Title series cleanup tests
./gradlew :app:testDebugUnitTest --tests "com.readle.app.util.TitleSeriesCleanupTest"

# All utility tests
./gradlew :app:testDebugUnitTest --tests "com.readle.app.util.*"
```

### Run Specific Test Method

```bash
./gradlew :app:testDebugUnitTest --tests "*normalizeTitle*"
```

### Run with Test Report

After running tests, open the HTML report:
```bash
open app/build/reports/tests/testDebugUnitTest/index.html
```

## Test Structure

### TextNormalizerTest (52 Tests)

Tests for normalizing book titles and author names:

#### Title Normalization (27 Tests)
- Moves articles to the end (e.g., "The Great Gatsby" → "Great Gatsby, The")
- Supports multiple languages:
  - German: Der, Die, Das
  - English: The
  - French: Le, La, Les, L'
- Handles edge cases: empty strings, special characters, etc.

#### Author Normalization (25 Tests)
- Converts to "LastName, FirstName" format
- Handles multiple authors separated by semicolons
- Preserves initials (e.g., "J.K. Rowling" → "Rowling, J.K.")
- Supports names with hyphens, apostrophes, and umlauts

### AuthorSeparatorConverterTest (32 Tests)

Tests for converting author separator formats from Audiobookshelf:

#### Format 1: FirstName LastName, FirstName LastName
- Replaces all ", " with "; "
- Example: "John Doe, Jane Smith" → "John Doe; Jane Smith"

#### Format 2: LastName, FirstName, LastName, FirstName
- Replaces every 2nd, 4th, 6th... comma with semicolon
- Example: "Doe, John, Smith, Jane" → "Doe, John; Smith, Jane"

### TitleSeriesCleanupTest (46 Tests)

Tests for removing series names and numbers from book titles:

#### Series with Articles (7 Tests)
- Handles series names that start with articles (Der, Die, Das, The)
- Example: "Donnerstagsmordclub 5: Die letzte Teufelsnummer, Der" → "letzte Teufelsnummer, Die"

#### Series without Articles (5 Tests)
- Removes series name and number, keeps subtitle
- Example: "Foundation #7 - The Search for Earth" → "Search for Earth, The"

#### Omnibus/Collections (4 Tests)
- Detects and preserves omnibus titles (e.g., "Foundation 1-3")
- Special handling for book #1 without subtitle

## Test Coverage

The test suite covers:

- Normal cases (happy path)
- Edge cases (empty strings, special characters, etc.)
- Unicode support (German umlauts, etc.)
- Whitespace handling
- Case sensitivity
- Multiple items (multiple authors, series, etc.)
- Real-world examples from actual book data

## Adding New Tests

### Adding a Test to Existing Suite

1. Open the test file (e.g., `app/src/test/java/com/readle/app/util/TextNormalizerTest.kt`)

2. Add a new test method:

```kotlin
@Test
fun `descriptive test name with spaces`() {
    val result = TextNormalizer.normalizeTitle("Input")
    assertEquals("Expected Output", result)
}
```

3. Run the specific test:

```bash
./gradlew :app:testDebugUnitTest --tests "*descriptive test name*"
```

### Test Naming Convention

Use descriptive names with backticks:
```kotlin
@Test
fun `normalizeTitle - German article Der`() { ... }
```

This makes test output more readable:
```
TextNormalizerTest > normalizeTitle - German article Der PASSED
```

## Continuous Integration

These tests can be integrated into CI/CD pipelines:

```yaml
# Example for GitHub Actions
- name: Run tests
  run: ./gradlew :app:testDebugUnitTest
```

## Troubleshooting

### Test Fails
1. Check the error message in terminal
2. Open HTML report: `app/build/reports/tests/testDebugUnitTest/index.html`
3. Compare expected vs. actual output
4. Fix either the test or the implementation

### Build Cache Issues
```bash
./gradlew clean
./gradlew :app:testDebugUnitTest
```

### IDE Not Running Tests
- Sync Gradle files
- Invalidate caches and restart
- Check that test files are in `src/test/java/` directory

## Best Practices

- Run tests before committing code
- Add tests for new features
- Update tests when changing behavior
- Use descriptive test names
- Test edge cases, not just happy path
- Keep tests fast and independent

## Implementation Details

### Test Utilities Location

- **TextNormalizer**: `app/src/main/java/com/readle/app/util/TextNormalizer.kt`
- **AuthorSeparatorConverter**: `app/src/main/java/com/readle/app/util/AuthorSeparatorConverter.kt`
- **TitleSeriesCleanup**: `app/src/main/java/com/readle/app/util/TitleSeriesCleanup.kt`

### Test Files Location

- **TextNormalizerTest**: `app/src/test/java/com/readle/app/util/TextNormalizerTest.kt`
- **AuthorSeparatorConverterTest**: `app/src/test/java/com/readle/app/util/AuthorSeparatorConverterTest.kt`
- **TitleSeriesCleanupTest**: `app/src/test/java/com/readle/app/util/TitleSeriesCleanupTest.kt`

## Test Statistics

- **Total Tests**: 130+
- **TextNormalizerTest**: 52 tests
- **AuthorSeparatorConverterTest**: 32 tests
- **TitleSeriesCleanupTest**: 46 tests

All tests pass successfully.
