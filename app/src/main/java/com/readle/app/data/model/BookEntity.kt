package com.readle.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val isbn: String? = null,
    val originalTitle: String? = null,
    val originalAuthor: String? = null,
    val description: String? = null,
    val publishDate: String? = null,
    val language: String? = null,
    val originalLanguage: String? = null,
    val series: String? = null,
    val seriesNumber: String? = null,
    val isEBook: Boolean = false,
    val comments: String? = null,
    val rating: Int = 0,
    val isOwned: Boolean = true,  // Replaces category: tracks if user owns the book
    val isRead: Boolean = false,  // Tracks if user has read the book
    val dateAdded: Long = System.currentTimeMillis(),
    val dateStarted: Long? = null,
    val dateFinished: Long? = null,
    val audiobookshelfId: String? = null,
    val inPocketbookCloud: Boolean = false, // Deprecated - use uploadedToCloudApi or uploadedViaEmail
    val uploadedToCloudApi: Boolean = false,
    val uploadedViaEmail: Boolean = false,
    val titleSort: String = "" // Normalized title for sorting (articles and special chars removed)
)

