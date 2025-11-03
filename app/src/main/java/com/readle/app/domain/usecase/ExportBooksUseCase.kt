package com.readle.app.domain.usecase

import android.content.Context
import com.readle.app.data.model.BookEntity
import com.readle.app.data.model.ReadingCategory
import com.readle.app.data.preferences.ExportFormat
import com.readle.app.util.ImageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.StringWriter

class ExportBooksUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {

    suspend fun execute(books: List<BookEntity>, format: ExportFormat): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                
                val exportsDir = File(context.filesDir, "exports")
                if (!exportsDir.exists()) {
                    exportsDir.mkdirs()
                }

                val timestamp = System.currentTimeMillis()
                val fileExtension = when (format) {
                    ExportFormat.JSON -> "json"
                    ExportFormat.XML -> "xml"
                }
                val exportFile = File(exportsDir, "readle_export_$timestamp.$fileExtension")

                val dataContent = when (format) {
                    ExportFormat.JSON -> createJsonExport(books)
                    ExportFormat.XML -> createXmlExport(books)
                }

                exportFile.writeText(dataContent)
                
                
                Result.success(exportFile)
            } catch (e: Exception) {
                android.util.Log.e("ExportBooksUseCase", "Export failed: ${e.message}", e)
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    private fun createJsonExport(books: List<BookEntity>): String {
        val exportData = books.map { book ->
            mapOf(
                "id" to book.id,
                "title" to book.title,
                "author" to book.author,
                "isbn" to book.isbn,
                "originalTitle" to book.originalTitle,
                "originalAuthor" to book.originalAuthor,
                "description" to book.description,
                "publishDate" to book.publishDate,
                "language" to book.language,
                "originalLanguage" to book.originalLanguage,
                "series" to book.series,
                "seriesNumber" to book.seriesNumber,
                "isEBook" to book.isEBook,
                "comments" to book.comments,
                "rating" to book.rating,
                "isOwned" to book.isOwned,
                "isRead" to book.isRead,
                "dateAdded" to book.dateAdded,
                "dateStarted" to book.dateStarted,
                "dateFinished" to book.dateFinished,
                "audiobookshelfId" to book.audiobookshelfId,
                "uploadedToCloudApi" to book.uploadedToCloudApi,
                "uploadedViaEmail" to book.uploadedViaEmail
            )
        }

        val prettyGson = GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()  // Include null values in JSON
            .create()
        val json = prettyGson.toJson(mapOf("books" to exportData))
        return json
    }

    private fun createXmlExport(books: List<BookEntity>): String {
        val docFactory = DocumentBuilderFactory.newInstance()
        val docBuilder = docFactory.newDocumentBuilder()
        val doc: Document = docBuilder.newDocument()

        val rootElement = doc.createElement("books")
        doc.appendChild(rootElement)

        books.forEach { book ->
            val bookElement = doc.createElement("book")
            rootElement.appendChild(bookElement)

            addElement(doc, bookElement, "id", book.id.toString())
            addElement(doc, bookElement, "title", book.title)
            addElement(doc, bookElement, "author", book.author)
            addElement(doc, bookElement, "isbn", book.isbn)
            addElement(doc, bookElement, "originalTitle", book.originalTitle)
            addElement(doc, bookElement, "originalAuthor", book.originalAuthor)
            addElement(doc, bookElement, "description", book.description)
            addElement(doc, bookElement, "publishDate", book.publishDate)
            addElement(doc, bookElement, "language", book.language)
            addElement(doc, bookElement, "originalLanguage", book.originalLanguage)
            addElement(doc, bookElement, "series", book.series)
            addElement(doc, bookElement, "seriesNumber", book.seriesNumber?.toString())
            addElement(doc, bookElement, "isEBook", book.isEBook.toString())
            addElement(doc, bookElement, "comments", book.comments)
            addElement(doc, bookElement, "rating", book.rating.toString())
            addElement(doc, bookElement, "isOwned", book.isOwned.toString())
            addElement(doc, bookElement, "isRead", book.isRead.toString())
            addElement(doc, bookElement, "dateAdded", book.dateAdded.toString())
            addElement(doc, bookElement, "dateStarted", book.dateStarted?.toString())
            addElement(doc, bookElement, "dateFinished", book.dateFinished?.toString())
            addElement(doc, bookElement, "audiobookshelfId", book.audiobookshelfId)
            addElement(doc, bookElement, "uploadedToCloudApi", book.uploadedToCloudApi.toString())
            addElement(doc, bookElement, "uploadedViaEmail", book.uploadedViaEmail.toString())
        }

        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")

        val source = DOMSource(doc)
        val writer = StringWriter()
        val result = StreamResult(writer)
        transformer.transform(source, result)

        return writer.toString()
    }

    private fun addElement(doc: Document, parent: Element, name: String, value: String?) {
        if (value != null) {
            val element = doc.createElement(name)
            element.appendChild(doc.createTextNode(value))
            parent.appendChild(element)
        }
    }
}

