package com.readle.app.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Custom Newsstand icon based on Material Design.
 * Represents books on a shelf - perfect for "I own this" status.
 */
val Icons.Filled.Newsstand: ImageVector
    get() {
        if (_newsstand != null) {
            return _newsstand!!
        }
        _newsstand = materialIcon(name = "Filled.Newsstand") {
            materialPath {
                // Books on shelf icon path from Material Symbols
                moveTo(2.0f, 20.0f)
                horizontalLineToRelative(20.0f)
                verticalLineToRelative(2.0f)
                horizontalLineTo(2.0f)
                close()
                
                moveTo(4.0f, 18.0f)
                horizontalLineToRelative(3.0f)
                verticalLineTo(8.0f)
                horizontalLineTo(4.0f)
                close()
                
                moveTo(8.5f, 18.0f)
                horizontalLineToRelative(3.0f)
                verticalLineTo(5.0f)
                horizontalLineToRelative(-3.0f)
                close()
                
                moveTo(13.0f, 18.0f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(-7.0f)
                horizontalLineToRelative(-3.0f)
                close()
                
                moveTo(17.5f, 18.0f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(-10.0f)
                horizontalLineToRelative(-3.0f)
                close()
            }
        }
        return _newsstand!!
    }

private var _newsstand: ImageVector? = null

