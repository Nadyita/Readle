package com.readle.app.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Custom EbookReader icon.
 * Represents an eBook reader device with text lines and an "A" symbol.
 * Converted from SVG (scaled from 512x512 to 24x24).
 */
val Icons.Filled.EbookReader: ImageVector
    get() {
        if (_ebookReader != null) {
            return _ebookReader!!
        }
        _ebookReader = materialIcon(name = "Filled.EbookReader") {
            materialPath {
                // Device body outline
                moveTo(18.81f, 23.56f)
                horizontalLineTo(5.19f)
                curveToRelative(-0.66f, 0.0f, -1.2f, -0.54f, -1.2f, -1.2f)
                verticalLineTo(1.58f)
                curveToRelative(0.0f, -0.66f, 0.54f, -1.2f, 1.2f, -1.2f)
                horizontalLineToRelative(13.62f)
                curveToRelative(0.66f, 0.0f, 1.2f, 0.54f, 1.2f, 1.2f)
                verticalLineToRelative(20.78f)
                curveTo(20.01f, 23.02f, 19.47f, 23.56f, 18.81f, 23.56f)
                close()
            }
            materialPath {
                // Screen area (lighter filled rect)
                moveTo(5.59f, 1.98f)
                horizontalLineToRelative(12.82f)
                verticalLineToRelative(17.64f)
                horizontalLineTo(5.59f)
                close()
            }
            materialPath {
                // Home button circle
                moveTo(12.0f, 20.44f)
                curveToRelative(-0.65f, 0.0f, -1.18f, 0.53f, -1.18f, 1.18f)
                reflectiveCurveToRelative(0.53f, 1.18f, 1.18f, 1.18f)
                curveToRelative(0.65f, 0.0f, 1.18f, -0.53f, 1.18f, -1.18f)
                reflectiveCurveTo(12.65f, 20.44f, 12.0f, 20.44f)
                close()
            }
            materialPath {
                // Text lines (right side)
                moveTo(12.0f, 7.56f)
                horizontalLineToRelative(4.41f)
                verticalLineToRelative(-0.75f)
                horizontalLineTo(12.0f)
                close()
                moveTo(12.0f, 4.36f)
                horizontalLineToRelative(4.41f)
                verticalLineToRelative(-0.75f)
                horizontalLineTo(12.0f)
                close()
                moveTo(12.0f, 5.96f)
                horizontalLineToRelative(1.6f)
                verticalLineToRelative(-0.75f)
                horizontalLineTo(12.0f)
                close()
                // Text lines (left side)
                moveTo(7.59f, 9.16f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(-0.75f)
                horizontalLineToRelative(-4.0f)
                close()
                moveTo(7.59f, 12.36f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(-0.75f)
                horizontalLineToRelative(-4.0f)
                close()
                moveTo(7.59f, 10.76f)
                horizontalLineToRelative(5.21f)
                verticalLineToRelative(-0.75f)
                horizontalLineTo(7.59f)
                close()
                moveTo(7.59f, 16.36f)
                horizontalLineToRelative(2.4f)
                verticalLineToRelative(-0.75f)
                horizontalLineToRelative(-2.4f)
                close()
                moveTo(7.59f, 14.76f)
                horizontalLineToRelative(5.21f)
                verticalLineToRelative(-0.75f)
                horizontalLineTo(7.59f)
                close()
                moveTo(7.59f, 17.96f)
                horizontalLineTo(12.0f)
                verticalLineToRelative(-0.75f)
                horizontalLineTo(7.59f)
                close()
            }
            materialPath {
                // "A" letter for font size
                moveTo(7.5f, 7.56f)
                curveToRelative(0.2f, 0.05f, 0.4f, -0.07f, 0.46f, -0.27f)
                lineToRelative(0.13f, -0.52f)
                horizontalLineToRelative(1.82f)
                lineToRelative(0.13f, 0.52f)
                curveToRelative(0.04f, 0.17f, 0.2f, 0.28f, 0.36f, 0.28f)
                curveToRelative(0.03f, 0.0f, 0.06f, 0.0f, 0.09f, -0.01f)
                curveToRelative(0.2f, -0.05f, 0.32f, -0.25f, 0.27f, -0.46f)
                lineToRelative(-0.73f, -2.9f)
                curveToRelative(-0.09f, -0.35f, -0.4f, -0.59f, -0.75f, -0.59f)
                horizontalLineToRelative(-0.58f)
                curveToRelative(-0.36f, 0.0f, -0.67f, 0.24f, -0.75f, 0.59f)
                lineToRelative(-0.73f, 2.9f)
                curveTo(7.18f, 7.31f, 7.3f, 7.51f, 7.5f, 7.56f)
                close()
                moveTo(8.72f, 4.36f)
                horizontalLineToRelative(0.58f)
                curveToRelative(0.01f, 0.0f, 0.02f, 0.01f, 0.02f, 0.02f)
                lineToRelative(0.41f, 1.63f)
                horizontalLineToRelative(-1.44f)
                lineToRelative(0.41f, -1.63f)
                curveTo(8.7f, 4.37f, 8.71f, 4.36f, 8.72f, 4.36f)
                close()
            }
        }
        return _ebookReader!!
    }

private var _ebookReader: ImageVector? = null

