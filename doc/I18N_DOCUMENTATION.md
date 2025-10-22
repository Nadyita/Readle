# Internationalization (i18n) Documentation

## Overview

Readle supports multiple languages through Android's built-in internationalization system. The app automatically detects and uses the device's language setting.

## Supported Languages

- **English (en)** - Default language
- **German (de)** - Complete translation

## File Structure

```
app/src/main/res/
├── values/           # English (default)
│   └── strings.xml
└── values-de/        # German
    └── strings.xml
```

## How It Works

1. Android checks the device's language setting
2. Loads the appropriate string resources automatically
3. Falls back to English if the language is not available

No code changes needed - the app automatically uses the correct translations.

## Adding a New Language

### 1. Create Language-Specific Folder

Create a new resource folder for your language:

```
app/src/main/res/values-{language_code}/
```

Examples:
- `values-es/` for Spanish
- `values-fr/` for French
- `values-it/` for Italian
- `values-pt/` for Portuguese

### 2. Copy strings.xml

```bash
cp app/src/main/res/values/strings.xml \
   app/src/main/res/values-es/strings.xml
```

### 3. Translate String Contents

- Keep the `name` attributes unchanged
- Translate only the text content
- Maintain placeholders like `%d`, `%s`

Example:

```xml
<!-- English (values/strings.xml) -->
<string name="msg_books_deleted">%d books deleted</string>

<!-- German (values-de/strings.xml) -->
<string name="msg_books_deleted">%d Bücher gelöscht</string>

<!-- Spanish (values-es/strings.xml) -->
<string name="msg_books_deleted">%d libros eliminados</string>
```

### 4. Test

Change your device language and verify the translations appear correctly.

## Testing Language Support

### On Device/Emulator

1. Go to `Settings` → `System` → `Languages & input` → `Languages`
2. Add/Select your language
3. Open the app to see the translations

### Using ADB

```bash
# Set German
adb shell setprop persist.sys.locale de-DE
adb shell stop
adb shell start

# Set English
adb shell setprop persist.sys.locale en-US
adb shell stop
adb shell start
```

### In Android Studio Preview

1. Open any layout file
2. Use the locale selector in the preview
3. Select your language code (e.g., `de` for German)

## String Resource Usage in Code

All strings must be referenced using `stringResource()`:

```kotlin
// Correct
Text(stringResource(R.string.action_add_book))

// Wrong - hardcoded string
Text("Add Book")
```

## Language-Specific Resources

You can also provide language-specific:

- **Layouts**: Different layouts for RTL languages
- **Images**: Culturally appropriate images  
- **Dimensions**: Adjust text sizes for longer translations

## Best Practices

- All user-facing strings must be externalized
- No hardcoded strings in code
- Use proper placeholders (`%d`, `%s`)
- Organize strings by category with comments
- Include content descriptions for accessibility
- Test with different string lengths
- Consider RTL languages (Arabic, Hebrew)

## Common Pitfalls

### Placeholder Order

Some languages may need different word order. Use indexed placeholders:

```xml
<!-- English -->
<string name="msg_book_added">Added "%1$s" by %2$s</string>

<!-- German (different word order possible) -->
<string name="msg_book_added">"%1$s" von %2$s hinzugefügt</string>
```

### Plurals

Use plural resources for proper grammar:

```xml
<plurals name="number_of_books">
    <item quantity="one">%d book</item>
    <item quantity="other">%d books</item>
</plurals>
```

Usage in code:
```kotlin
pluralStringResource(R.plurals.number_of_books, count, count)
```

## Verification

Translation completeness is automatically verified by unit tests:

```bash
./gradlew :app:testDebugUnitTest --tests "com.readle.app.i18n.TranslationCompletenessTest"
```

This test checks:
- All string keys exist in all languages
- Placeholders match between languages
- All XML files are valid

Manual verification:
- UI tested on device with that language
- Layouts don't break with longer strings

## Resources

- [Android Localization Guide](https://developer.android.com/guide/topics/resources/localization)
- [Language Codes](https://developer.android.com/reference/java/util/Locale)
