# Readle - Book Management App

**Readle** is a modern Android application for managing your personal book library. Built with Kotlin, Jetpack Compose, and Material Design 3, it helps you organize your physical books and eBooks, track your reading progress, and transfer eBooks to your eBook reader.

## Features

### Book Management
- **Multiple Reading Categories**: Want to Read, Currently Reading, Read, and Did Not Finish
- **Comprehensive Book Information**: Store title, author, description, publisher, publish date, languages, series info, ISBN, and cover images
- **5-Star Rating System**: Rate books you've finished (only shown for "Read" category)
- **Search Functionality**: Quickly find books in your library
- **Context Menu**: Long-press any book to edit or delete it
- **API Source Badges**: See which API provided each book's data

### Book Entry Methods
- **ISBN Scanner**: Use your camera to scan book barcodes (powered by ML Kit)
- **Manual ISBN Entry**: Type in ISBN numbers directly
- **Title/Author Search**: Search by book title or author name

### Multi-API Support
- **Deutsche Nationalbibliothek (DNB) SRU API**: German book database with smart filtering
  - Only German-language books
  - Excludes audiobooks
  - Automatic cover images via ISBN
  - Detailed book descriptions from DNB content service
- **Google Books API**: Extensive international book database
  - Smart audiobook filtering
  - High-quality cover images
- **ISBN DB API**: Additional ISBN lookup service
- **Automatic Deduplication**: Same book from multiple APIs shown only once

### eBook Management
- **Transfer to eBook Reader**: Send eBooks directly to your eBook reader
- **eBook Organization**: Manage your digital book collection alongside physical books

### Import/Export
- **Multiple Formats**: Export data as JSON or XML
- **Cover Images Included**: Exports as ZIP files with all cover images
- **Flexible Import**: Choose to merge with existing data or replace all

### Modern UI
- **Material Design 3**: Beautiful, modern interface
- **Dark/Light Theme**: Switch between themes or follow system settings
- **Responsive Layout**: Optimized for different screen sizes
- **Internationalization (i18n)**: Full support for English and German

### Privacy-First
- **Local Storage Only**: All data stored on your device
- **No Account Required**: No sign-up, no tracking
- **Full Data Control**: Export and import your data anytime

## Technical Stack

### Architecture
- **MVVM Pattern**: Clean separation of concerns
- **Repository Pattern**: Abstraction over data sources
- **Use Cases**: Business logic isolation

### Core Technologies
- **Kotlin**: 100% Kotlin codebase
- **Jetpack Compose**: Modern declarative UI
- **Room Database**: Local data persistence
- **Retrofit**: Network communication
- **Hilt**: Dependency injection
- **Coroutines & Flow**: Asynchronous programming

### Libraries
- **ML Kit**: Barcode scanning
- **CameraX**: Camera integration
- **Coil**: Image loading
- **DataStore**: Preferences storage
- **Gson**: JSON serialization

### Testing
- **JUnit**: Unit testing framework
- **Mockito & MockK**: Mocking frameworks
- **Turbine**: Flow testing
- **Compose UI Testing**: UI component tests
- **Room Testing**: Database tests

## Requirements

- **Minimum SDK**: API 31 (Android 12)
- **Target SDK**: API 34 (Android 14)
- **Camera**: Required for ISBN scanning (optional feature)
- **Internet**: Required for book searches (app works offline for viewing existing library)
- **Storage**: Access required for eBook management and transfers
- **Languages**: English (en), German (de)

## Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd book-app
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned repository

3. **Build and Run**
   - Connect an Android device or start an emulator
   - Click "Run" or press Shift+F10

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/readle/app/
│   │   │   ├── data/
│   │   │   │   ├── api/          # API clients and models
│   │   │   │   ├── database/     # Room database and DAOs
│   │   │   │   ├── model/        # Data models
│   │   │   │   ├── preferences/  # DataStore preferences
│   │   │   │   └── repository/   # Data repositories
│   │   │   ├── di/               # Hilt dependency injection
│   │   │   ├── domain/
│   │   │   │   └── usecase/      # Business logic use cases
│   │   │   ├── scanner/          # Barcode scanning
│   │   │   ├── ui/
│   │   │   │   ├── navigation/   # Navigation setup
│   │   │   │   ├── screens/      # Compose screens
│   │   │   │   ├── theme/        # Material Design theme
│   │   │   │   └── viewmodel/    # ViewModels
│   │   │   ├── util/             # Utility classes
│   │   │   ├── MainActivity.kt
│   │   │   └── ReadleApplication.kt
│   │   └── res/                  # Resources (strings, XML, etc.)
│   ├── test/                     # Unit tests
│   └── androidTest/              # Integration & UI tests
```

## Usage

### Adding a Book

1. **Via Scanner**:
   - Tap the "+" button
   - Select "Scan ISBN" tab
   - Point camera at book's barcode
   - Select the correct book from results
   - Choose category (Want to Read, Currently Reading, Read, Did Not Finish)
   - Rate if marking as "Read"

2. **Via Manual Entry**:
   - Tap the "+" button
   - Select "Enter ISBN" tab
   - Type ISBN and tap "Search"
   - Select book from results
   - Choose category and optional rating

3. **Via Title/Author Search**:
   - Tap the "+" button
   - Select "Search by Title/Author" tab
   - Enter book details
   - Select book from results
   - Choose category and optional rating

### Managing Books

- **Edit**: Tap on any book to edit its details
- **Move Categories**: Change the category in edit screen
- **Delete Single Book**: Long-press a book and select "Delete" from the context menu
- **Delete Multiple Books**: Long-press to select, then delete multiple books
- **Search**: Use the search icon to find books in your library

### Settings

- **Theme**: Light, Dark, or System
- **Scanner Library**: Choose between ML Kit and ZXing
- **Export Format**: JSON or XML
- **Import/Export**: Backup and restore your library
- **Language**: Automatically uses device language (English/German)

## API Configuration

### ISBNdb API (Optional)
To enable ISBNdb support, add your API key in:
```kotlin
// app/src/main/java/com/readle/app/data/api/isbndb/IsbnDbApiClient.kt
private val apiKey = "YOUR_API_KEY_HERE"
```

## Testing

### Run Unit Tests
```bash
./gradlew test
```

Specific tests can be run with:
```bash
./gradlew :app:testDebugUnitTest
```

### Run Integration Tests
```bash
./gradlew connectedAndroidTest
```

## Build

### Debug Build
```bash
./gradlew assembleDebug
```

### Install on Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
# or
./gradlew installDebug
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- **Deutsche Nationalbibliothek** for the SRU API
- **Google Books API** for book data
- **ISBNdb** for additional book information
- **Material Design** for design guidelines

## Recent Improvements

- **Smart Filtering**: DNB shows only German-language books, both DNB and Google Books filter out audiobooks
- **Automatic Deduplication**: Same book from multiple APIs shown only once
- **Enhanced DNB Support**: Automatic cover images via ISBN and detailed book descriptions
- **Better UX**: Rating selector only shown for books marked as "Read"
- **Context Menu**: Long-press books for quick edit/delete actions
- **API Source Badges**: See which API provided each search result
- **Improved Export**: Fixed ZIP file generation with proper flush handling
- **Clean Data**: Removed MARC21 control characters from titles and dates

---

Built with Kotlin and Jetpack Compose
