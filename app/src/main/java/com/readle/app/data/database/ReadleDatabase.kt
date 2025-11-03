package com.readle.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.readle.app.data.model.BookEntity

@Database(
    entities = [BookEntity::class],
    version = 11,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ReadleDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        const val DATABASE_NAME = "readle_database"
        
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table with updated schema
                database.execSQL("""
                    CREATE TABLE books_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        author TEXT NOT NULL,
                        description TEXT,
                        publishDate TEXT,
                        language TEXT,
                        originalLanguage TEXT,
                        series TEXT,
                        seriesNumber INTEGER,
                        isEBook INTEGER NOT NULL DEFAULT 0,
                        comments TEXT,
                        rating INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        isRead INTEGER NOT NULL,
                        dateAdded INTEGER NOT NULL,
                        dateStarted INTEGER,
                        dateFinished INTEGER
                    )
                """.trimIndent())
                
                // Migrate data and map old categories to new ones
                // WANT_TO_READ -> WANT, CURRENTLY_READING -> OWN, READ -> READ
                database.execSQL("""
                    INSERT INTO books_new (
                        id, title, author, description, publishDate, language,
                        originalLanguage, series, seriesNumber, isEBook, comments,
                        rating, category, isRead, dateAdded, dateStarted, dateFinished
                    )
                    SELECT 
                        id, title, author, description, publishDate, language,
                        originalLanguage, series, seriesNumber, 0 as isEBook, NULL as comments,
                        rating,
                        CASE category
                            WHEN 'WANT_TO_READ' THEN 'WANT'
                            WHEN 'CURRENTLY_READING' THEN 'OWN'
                            WHEN 'READ' THEN 'READ'
                            ELSE 'WANT'
                        END as category,
                        isRead, dateAdded, dateStarted, dateFinished
                    FROM books
                """.trimIndent())
                
                // Drop old table
                database.execSQL("DROP TABLE books")
                
                // Rename new table
                database.execSQL("ALTER TABLE books_new RENAME TO books")
            }
        }
        
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add audiobookshelfId column
                database.execSQL("ALTER TABLE books ADD COLUMN audiobookshelfId TEXT")
            }
        }
        
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Change seriesNumber from INTEGER to TEXT to support decimals (e.g., "4.5")
                // SQLite doesn't support ALTER COLUMN, so we need to recreate the table
                
                // Create new table with TEXT seriesNumber
                database.execSQL("""
                    CREATE TABLE books_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        author TEXT NOT NULL,
                        description TEXT,
                        publishDate TEXT,
                        language TEXT,
                        originalLanguage TEXT,
                        series TEXT,
                        seriesNumber TEXT,
                        isEBook INTEGER NOT NULL DEFAULT 0,
                        comments TEXT,
                        rating INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        isRead INTEGER NOT NULL,
                        dateAdded INTEGER NOT NULL,
                        dateStarted INTEGER,
                        dateFinished INTEGER,
                        audiobookshelfId TEXT
                    )
                """.trimIndent())
                
                // Copy data, converting INTEGER seriesNumber to TEXT
                database.execSQL("""
                    INSERT INTO books_new (
                        id, title, author, description, publishDate, language,
                        originalLanguage, series, seriesNumber, isEBook, comments,
                        rating, category, isRead, dateAdded, dateStarted, dateFinished,
                        audiobookshelfId
                    )
                    SELECT 
                        id, title, author, description, publishDate, language,
                        originalLanguage, series, 
                        CAST(seriesNumber AS TEXT) as seriesNumber,
                        isEBook, comments, rating, category, isRead, 
                        dateAdded, dateStarted, dateFinished, audiobookshelfId
                    FROM books
                """.trimIndent())
                
                // Drop old table
                database.execSQL("DROP TABLE books")
                
                // Rename new table
                database.execSQL("ALTER TABLE books_new RENAME TO books")
            }
        }
        
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add originalTitle and originalAuthor columns for bidirectional search
                database.execSQL("ALTER TABLE books ADD COLUMN originalTitle TEXT")
                database.execSQL("ALTER TABLE books ADD COLUMN originalAuthor TEXT")
            }
        }
        
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Populate originalTitle and originalAuthor from normalized values
                // (reverse normalization for existing books)
                
                // For titles: "Haarteppichknüpfer, Die" -> "Die Haarteppichknüpfer"
                database.execSQL("""
                    UPDATE books 
                    SET originalTitle = 
                        CASE 
                            WHEN title LIKE '%, Der' THEN 'Der ' || SUBSTR(title, 1, LENGTH(title) - 5)
                            WHEN title LIKE '%, Die' THEN 'Die ' || SUBSTR(title, 1, LENGTH(title) - 5)
                            WHEN title LIKE '%, Das' THEN 'Das ' || SUBSTR(title, 1, LENGTH(title) - 5)
                            WHEN title LIKE '%, The' THEN 'The ' || SUBSTR(title, 1, LENGTH(title) - 5)
                            WHEN title LIKE '%, Le' THEN 'Le ' || SUBSTR(title, 1, LENGTH(title) - 4)
                            WHEN title LIKE '%, La' THEN 'La ' || SUBSTR(title, 1, LENGTH(title) - 4)
                            WHEN title LIKE '%, Les' THEN 'Les ' || SUBSTR(title, 1, LENGTH(title) - 5)
                            WHEN title LIKE '%, L''' THEN 'L''' || SUBSTR(title, 1, LENGTH(title) - 4)
                            ELSE title
                        END
                    WHERE originalTitle IS NULL
                """.trimIndent())
                
                // For authors: "Eschbach, Andreas" -> "Andreas Eschbach"
                database.execSQL("""
                    UPDATE books 
                    SET originalAuthor = 
                        CASE 
                            WHEN author LIKE '%,%' THEN 
                                TRIM(SUBSTR(author, INSTR(author, ',') + 1)) || ' ' || TRIM(SUBSTR(author, 1, INSTR(author, ',') - 1))
                            ELSE author
                        END
                    WHERE originalAuthor IS NULL
                """.trimIndent())
            }
        }
        
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add isbn column
                database.execSQL("ALTER TABLE books ADD COLUMN isbn TEXT")
            }
        }
        
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add inPocketbookCloud column
                database.execSQL("ALTER TABLE books ADD COLUMN inPocketbookCloud INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add uploadedToCloudApi and uploadedViaEmail columns
                // Copy existing inPocketbookCloud values to uploadedToCloudApi
                database.execSQL("ALTER TABLE books ADD COLUMN uploadedToCloudApi INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE books ADD COLUMN uploadedViaEmail INTEGER NOT NULL DEFAULT 0")
                
                // Migrate existing inPocketbookCloud values to uploadedToCloudApi
                // (we assume existing uploads were via Cloud API)
                database.execSQL("UPDATE books SET uploadedToCloudApi = inPocketbookCloud")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Replace category enum with isOwned and isRead boolean flags
                // Migration strategy:
                //   WANT -> isOwned=false, isRead=false
                //   OWN  -> isOwned=true,  isRead=false
                //   READ -> isOwned=true,  isRead=true
                
                // Add new columns
                database.execSQL("ALTER TABLE books ADD COLUMN isOwned INTEGER NOT NULL DEFAULT 1")
                
                // Migrate data based on category
                database.execSQL("""
                    UPDATE books 
                    SET isOwned = CASE category
                        WHEN 'WANT' THEN 0
                        WHEN 'OWN' THEN 1
                        WHEN 'READ' THEN 1
                        ELSE 1
                    END
                """.trimIndent())
                
                // Note: isRead column already exists from earlier schema
                // Update isRead based on category for READ books
                database.execSQL("""
                    UPDATE books 
                    SET isRead = CASE category
                        WHEN 'READ' THEN 1
                        ELSE 0
                    END
                """.trimIndent())
                
                // We keep the category column for now to avoid data loss
                // It can be removed in a future migration if needed
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add titleSort column for locale-aware sorting
                // This field contains the title with articles and special characters removed
                // based on the book's language
                database.execSQL("ALTER TABLE books ADD COLUMN titleSort TEXT NOT NULL DEFAULT ''")
                
                // Populate titleSort for existing books
                // We use a simplified normalization in SQL (remove common articles)
                // The app will recalculate proper values on next book update/import
                database.execSQL("""
                    UPDATE books 
                    SET titleSort = 
                        CASE
                            -- Remove leading special characters
                            WHEN SUBSTR(title, 1, 1) IN ('¿', '¡', '"', '''', '«', '»') 
                                THEN TRIM(SUBSTR(title, 2))
                            -- Remove German articles (Der/Die/Das/Ein/Eine)
                            WHEN UPPER(SUBSTR(title, 1, 4)) = 'DER ' THEN TRIM(SUBSTR(title, 5))
                            WHEN UPPER(SUBSTR(title, 1, 4)) = 'DIE ' THEN TRIM(SUBSTR(title, 5))
                            WHEN UPPER(SUBSTR(title, 1, 4)) = 'DAS ' THEN TRIM(SUBSTR(title, 5))
                            WHEN UPPER(SUBSTR(title, 1, 4)) = 'EIN ' THEN TRIM(SUBSTR(title, 5))
                            WHEN UPPER(SUBSTR(title, 1, 5)) = 'EINE ' THEN TRIM(SUBSTR(title, 6))
                            -- Remove English articles (The/A/An)
                            WHEN UPPER(SUBSTR(title, 1, 4)) = 'THE ' THEN TRIM(SUBSTR(title, 5))
                            WHEN UPPER(SUBSTR(title, 1, 2)) = 'A ' THEN TRIM(SUBSTR(title, 3))
                            WHEN UPPER(SUBSTR(title, 1, 3)) = 'AN ' THEN TRIM(SUBSTR(title, 4))
                            -- Remove Spanish articles (El/La/Los/Las/Un/Una)
                            WHEN UPPER(SUBSTR(title, 1, 3)) = 'EL ' THEN TRIM(SUBSTR(title, 4))
                            WHEN UPPER(SUBSTR(title, 1, 3)) = 'LA ' THEN TRIM(SUBSTR(title, 4))
                            WHEN UPPER(SUBSTR(title, 1, 4)) = 'LOS ' THEN TRIM(SUBSTR(title, 5))
                            WHEN UPPER(SUBSTR(title, 1, 4)) = 'LAS ' THEN TRIM(SUBSTR(title, 5))
                            WHEN UPPER(SUBSTR(title, 1, 3)) = 'UN ' THEN TRIM(SUBSTR(title, 4))
                            WHEN UPPER(SUBSTR(title, 1, 4)) = 'UNA ' THEN TRIM(SUBSTR(title, 5))
                            -- Remove French articles (Le/La/Les/L'/Un/Une)
                            WHEN UPPER(SUBSTR(title, 1, 3)) = 'LE ' THEN TRIM(SUBSTR(title, 4))
                            WHEN UPPER(SUBSTR(title, 1, 4)) = 'LES ' THEN TRIM(SUBSTR(title, 5))
                            WHEN UPPER(SUBSTR(title, 1, 2)) = 'L''' THEN TRIM(SUBSTR(title, 3))
                            -- Keep title as-is if no article detected
                            ELSE title
                        END
                """.trimIndent())
            }
        }
    }
}
