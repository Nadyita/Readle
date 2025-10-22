package com.readle.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.readle.app.data.model.BookEntity
import com.readle.app.data.model.ReadingCategory
import com.readle.app.ui.theme.ReadleTheme
import org.junit.Rule
import org.junit.Test

class BookListItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testBook = BookEntity(
        id = 1,
        title = "Test Book",
        author = "Test Author",
        description = "Test Description",
        publisher = "Test Publisher",
        publishDate = "2024",
        language = "de",
        originalLanguage = null,
        series = null,
        seriesNumber = null,
        isbn = "1234567890",
        coverPath = null,
        rating = 4,
        category = ReadingCategory.WANT_TO_READ,
        isRead = false
    )

    @Test
    fun bookListItem_displaysBookInformation() {
        var clicked = false

        composeTestRule.setContent {
            ReadleTheme {
                BookListItem(
                    book = testBook,
                    isSelected = false,
                    onBookClick = { clicked = true },
                    onBookLongClick = {},
                    onSelectionToggle = {},
                    showCheckbox = false
                )
            }
        }

        composeTestRule.onNodeWithText("Test Book").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Author").assertIsDisplayed()

        composeTestRule.onNodeWithText("Test Book").performClick()
        assert(clicked)
    }

    @Test
    fun bookListItem_showsCheckboxWhenInSelectionMode() {
        composeTestRule.setContent {
            ReadleTheme {
                BookListItem(
                    book = testBook,
                    isSelected = true,
                    onBookClick = {},
                    onBookLongClick = {},
                    onSelectionToggle = {},
                    showCheckbox = true
                )
            }
        }

        composeTestRule.onNodeWithText("Test Book").assertIsDisplayed()
    }
}

