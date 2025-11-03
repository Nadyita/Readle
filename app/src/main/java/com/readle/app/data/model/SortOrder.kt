package com.readle.app.data.model

enum class SortOrder(val displayNameResId: Int) {
    TITLE_ASC(com.readle.app.R.string.sort_title_asc),
    TITLE_DESC(com.readle.app.R.string.sort_title_desc),
    AUTHOR_ASC(com.readle.app.R.string.sort_author_asc),
    AUTHOR_DESC(com.readle.app.R.string.sort_author_desc),
    DATE_ADDED_ASC(com.readle.app.R.string.sort_date_added_asc),
    DATE_ADDED_DESC(com.readle.app.R.string.sort_date_added_desc);

    fun toSqlOrderBy(): String {
        return when (this) {
            TITLE_ASC -> "title ASC"
            TITLE_DESC -> "title DESC"
            AUTHOR_ASC -> "author ASC"
            AUTHOR_DESC -> "author DESC"
            DATE_ADDED_ASC -> "dateAdded ASC"
            DATE_ADDED_DESC -> "dateAdded DESC"
        }
    }

    companion object {
        fun fromString(value: String?): SortOrder {
            return value?.let { 
                try { 
                    valueOf(it) 
                } catch (e: IllegalArgumentException) { 
                    TITLE_ASC 
                }
            } ?: TITLE_ASC
        }
    }
}


