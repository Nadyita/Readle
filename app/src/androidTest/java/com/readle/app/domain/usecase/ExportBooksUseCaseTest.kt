package com.readle.app.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.readle.app.data.model.BookEntity
import com.readle.app.data.model.ReadingCategory
import com.readle.app.data.preferences.ExportFormat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.zip.ZipFile

@RunWith(AndroidJUnit4::class)
class ExportBooksUseCaseTest {

    private lateinit var context: Context
    private lateinit var exportBooksUseCase: ExportBooksUseCase
    private lateinit var gson: Gson

    private val testBooks = listOf(
        BookEntity(
            id = 1,
            title = "Test Book 1",
            author = "Test Author 1",
            description = "Description 1",
            publisher = "Publisher 1",
            publishDate = "2024",
            language = "de",
            originalLanguage = "en",
            series = null,
            seriesNumber = null,
            isbn = "1234567890",
            coverPath = null,
            rating = 5,
            category = ReadingCategory.READ,
            isRead = true
        ),
        BookEntity(
            id = 2,
            title = "Test Book 2",
            author = "Test Author 2",
            description = "Description 2",
            publisher = "Publisher 2",
            publishDate = "2023",
            language = "en",
            originalLanguage = null,
            series = null,
            seriesNumber = null,
            isbn = "0987654321",
            coverPath = null,
            rating = 4,
            category = ReadingCategory.WANT_TO_READ,
            isRead = false
        )
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        gson = Gson()
        exportBooksUseCase = ExportBooksUseCase(context, gson)
    }

    @Test
    fun exportBooks_asJSON_createsValidZipFile() = runBlocking {
        val result = exportBooksUseCase.execute(testBooks, ExportFormat.JSON)

        assertTrue(result.isSuccess)

        val file = result.getOrNull()
        assertTrue(file != null)
        assertTrue(file!!.exists())
        assertTrue(file.name.startsWith("readle_export_"))
        assertTrue(file.extension == "zip")

        val zipFile = ZipFile(file)
        val entries = zipFile.entries().toList()
        assertTrue(entries.any { it.name == "books.json" })

        file.delete()
    }

    @Test
    fun exportBooks_asXML_createsValidZipFile() = runBlocking {
        val result = exportBooksUseCase.execute(testBooks, ExportFormat.XML)

        assertTrue(result.isSuccess)

        val file = result.getOrNull()
        assertTrue(file != null)
        assertTrue(file!!.exists())

        val zipFile = ZipFile(file)
        val entries = zipFile.entries().toList()
        assertTrue(entries.any { it.name == "books.xml" })

        file.delete()
    }

    @Test
    fun exportBooks_withEmptyList_createsValidZipFile() = runBlocking {
        val result = exportBooksUseCase.execute(emptyList(), ExportFormat.JSON)

        assertTrue(result.isSuccess)

        val file = result.getOrNull()
        assertTrue(file != null)
        assertTrue(file!!.exists())

        file.delete()
    }
}

