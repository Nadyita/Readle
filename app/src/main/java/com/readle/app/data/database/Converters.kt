package com.readle.app.data.database

import androidx.room.TypeConverter
import com.readle.app.data.model.ReadingCategory

class Converters {

    @TypeConverter
    fun fromReadingCategory(category: ReadingCategory): String {
        return category.name
    }

    @TypeConverter
    fun toReadingCategory(value: String): ReadingCategory {
        return ReadingCategory.valueOf(value)
    }
}

