package com.readle.app.ui.navigation

sealed class Screen(val route: String) {
    object BookList : Screen("book_list")
    object AddBook : Screen("add_book")
    object EditBook : Screen("edit_book/{bookId}") {
        fun createRoute(bookId: Long) = "edit_book/$bookId"
    }
    object Scanner : Screen("scanner")
    object Settings : Screen("settings")
    object EmailSettings : Screen("email_settings")
}

