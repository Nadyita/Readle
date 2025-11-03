package com.readle.app.domain.usecase

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.readle.app.data.model.BookEntity
import com.readle.app.data.model.ReadingCategory
import com.readle.app.data.repository.BookRepository
import com.readle.app.util.ImageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

class ImportBooksUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val gson: Gson
) {

    suspend fun execute(zipFile: File, replaceExisting: Boolean): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                if (replaceExisting) {
                    bookRepository.deleteAllBooks()
                    ImageManager.getImagesDirectory(context).listFiles()?.forEach { it.delete() }
                }

                val tempDir = File(context.cacheDir, "import_${System.currentTimeMillis()}")
                tempDir.mkdirs()

                ZipInputStream(FileInputStream(zipFile)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val file = File(tempDir, entry.name)
                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile?.mkdirs()
                            file.outputStream().use { output ->
                                zipIn.copyTo(output)
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }

                val jsonFile = File(tempDir, "books.json")
                val xmlFile = File(tempDir, "books.xml")

                val books = when {
                    jsonFile.exists() -> parseJsonFile(jsonFile, tempDir)
                    xmlFile.exists() -> parseXmlFile(xmlFile, tempDir)
                    else -> throw Exception("No valid data file found in archive")
                }

                bookRepository.insertBooks(books)

                tempDir.deleteRecursively()

                Result.success(books.size)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    private suspend fun parseJsonFile(file: File, tempDir: File): List<BookEntity> {
        val jsonString = file.readText()
        val type = object : TypeToken<Map<String, List<Map<String, Any?>>>>() {}.type
        val data: Map<String, List<Map<String, Any?>>> = gson.fromJson(jsonString, type)

        val bookMaps = data["books"] ?: emptyList()

        return bookMaps.map { bookMap ->
            // Cover handling removed - no longer storing covers

            BookEntity(
                id = 0,
                title = bookMap["title"] as? String ?: "",
                author = bookMap["author"] as? String ?: "",
                isbn = bookMap["isbn"] as? String,
                originalTitle = bookMap["originalTitle"] as? String,
                originalAuthor = bookMap["originalAuthor"] as? String,
                description = bookMap["description"] as? String,
                publishDate = bookMap["publishDate"] as? String,
                language = bookMap["language"] as? String,
                originalLanguage = bookMap["originalLanguage"] as? String,
                series = bookMap["series"] as? String,
                seriesNumber = when (val num = bookMap["seriesNumber"]) {
                    is Number -> num.toString()
                    is String -> num
                    else -> null
                },
                isEBook = bookMap["isEBook"] as? Boolean ?: false,
                comments = bookMap["comments"] as? String,
                rating = (bookMap["rating"] as? Double)?.toInt() ?: 0,
                isOwned = bookMap["isOwned"] as? Boolean ?: true,  // Default to owned for imports
                isRead = bookMap["isRead"] as? Boolean ?: false,
                dateAdded = (bookMap["dateAdded"] as? Double)?.toLong() ?: System.currentTimeMillis(),
                dateStarted = (bookMap["dateStarted"] as? Double)?.toLong(),
                dateFinished = (bookMap["dateFinished"] as? Double)?.toLong(),
                audiobookshelfId = bookMap["audiobookshelfId"] as? String,
                uploadedToCloudApi = bookMap["uploadedToCloudApi"] as? Boolean ?: false,
                uploadedViaEmail = bookMap["uploadedViaEmail"] as? Boolean ?: false,
                titleSort = com.readle.app.util.TextNormalizer.normalizeTitleForSorting(
                    bookMap["title"] as? String ?: "",
                    bookMap["language"] as? String
                )
            )
        }
    }

    private suspend fun parseXmlFile(file: File, tempDir: File): List<BookEntity> {
        val books = mutableListOf<BookEntity>()

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)

        val bookNodes = doc.getElementsByTagName("book")

        for (i in 0 until bookNodes.length) {
            val bookNode = bookNodes.item(i) as? Element ?: continue

            // Cover handling removed - no longer storing covers

            val book = BookEntity(
                id = 0,
                title = getElementText(bookNode, "title") ?: "",
                author = getElementText(bookNode, "author") ?: "",
                isbn = getElementText(bookNode, "isbn"),
                originalTitle = getElementText(bookNode, "originalTitle"),
                originalAuthor = getElementText(bookNode, "originalAuthor"),
                description = getElementText(bookNode, "description"),
                publishDate = getElementText(bookNode, "publishDate"),
                language = getElementText(bookNode, "language"),
                originalLanguage = getElementText(bookNode, "originalLanguage"),
                series = getElementText(bookNode, "series"),
                seriesNumber = getElementText(bookNode, "seriesNumber"),
                isEBook = getElementText(bookNode, "isEBook")?.toBoolean() ?: false,
                comments = getElementText(bookNode, "comments"),
                rating = getElementText(bookNode, "rating")?.toIntOrNull() ?: 0,
                isOwned = getElementText(bookNode, "isOwned")?.toBoolean() ?: true,  // Default to owned for imports
                isRead = getElementText(bookNode, "isRead")?.toBoolean() ?: false,
                dateAdded = getElementText(bookNode, "dateAdded")?.toLongOrNull()
                    ?: System.currentTimeMillis(),
                dateStarted = getElementText(bookNode, "dateStarted")?.toLongOrNull(),
                dateFinished = getElementText(bookNode, "dateFinished")?.toLongOrNull(),
                audiobookshelfId = getElementText(bookNode, "audiobookshelfId"),
                uploadedToCloudApi = getElementText(bookNode, "uploadedToCloudApi")?.toBoolean() ?: false,
                uploadedViaEmail = getElementText(bookNode, "uploadedViaEmail")?.toBoolean() ?: false,
                titleSort = com.readle.app.util.TextNormalizer.normalizeTitleForSorting(
                    getElementText(bookNode, "title") ?: "",
                    getElementText(bookNode, "language")
                )
            )
            books.add(book)
        }

        return books
    }

    private fun getElementText(parent: Element, tagName: String): String? {
        val nodes = parent.getElementsByTagName(tagName)
        return if (nodes.length > 0) {
            nodes.item(0).textContent
        } else null
    }

    private suspend fun copyCoverImage(sourceFile: File): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (!sourceFile.exists()) return@withContext null

                val imagesDir = ImageManager.getImagesDirectory(context)
                val destFile = File(imagesDir, sourceFile.name)

                sourceFile.copyTo(destFile, overwrite = true)
                destFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}

