package com.readle.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.readle.app.ui.screens.AddBookScreen
import com.readle.app.ui.screens.BookListScreen
import com.readle.app.ui.screens.EditBookScreen
import com.readle.app.ui.screens.ScannerScreen
import com.readle.app.ui.screens.SettingsScreen

@Composable
fun ReadleNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.BookList.route
    ) {
        composable(Screen.BookList.route) { backStackEntry ->
            val filterText = backStackEntry.savedStateHandle.get<String>("filter_text")
            BookListScreen(
                onNavigateToAddBook = {
                    navController.navigate(Screen.AddBook.route)
                },
                onNavigateToEditBook = { bookId ->
                    navController.navigate(Screen.EditBook.createRoute(bookId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                initialFilterText = filterText
            )
            
            // Clear the filter text after it's been used
            if (filterText != null) {
                backStackEntry.savedStateHandle.remove<String>("filter_text")
            }
        }

        composable(Screen.AddBook.route) { backStackEntry ->
            val scannedIsbn = backStackEntry.savedStateHandle.get<String>("scanned_isbn")
            AddBookScreen(
                scannedIsbn = scannedIsbn,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToScanner = {
                    navController.navigate(Screen.Scanner.route)
                },
                onIsbnProcessed = {
                    backStackEntry.savedStateHandle.remove<String>("scanned_isbn")
                }
            )
        }

        composable(
            route = Screen.EditBook.route,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
            val bookListEntry = navController.getBackStackEntry(Screen.BookList.route)
            
            EditBookScreen(
                bookId = bookId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onFilterByText = { filterText ->
                    // Save the filter text to the BookList's savedStateHandle
                    bookListEntry.savedStateHandle["filter_text"] = filterText
                }
            )
        }

        composable(Screen.Scanner.route) {
            ScannerScreen(
                onBarcodeDetected = { isbn ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("scanned_isbn", isbn)
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEmailSettings = {
                    navController.navigate(Screen.EmailSettings.route)
                }
            )
        }

        composable(Screen.EmailSettings.route) {
            com.readle.app.ui.screens.EmailSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

