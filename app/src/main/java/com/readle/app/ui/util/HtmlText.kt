package com.readle.app.ui.util

import android.text.Spanned
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat

/**
 * Converts HTML string to AnnotatedString for Compose Text
 * Also handles plain text with newlines correctly
 */
fun String.htmlToAnnotatedString(): AnnotatedString {
    // Convert newlines to double line breaks for proper spacing
    val htmlWithBreaks = this.replace("\n", "<br><br>")
    val spanned = HtmlCompat.fromHtml(htmlWithBreaks, HtmlCompat.FROM_HTML_MODE_COMPACT)
    return spanned.toAnnotatedString()
}

/**
 * Converts Android Spanned (from Html.fromHtml) to Compose AnnotatedString
 */
fun Spanned.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    val spanned = this@toAnnotatedString
    append(spanned.toString())
    
    spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
        val start = spanned.getSpanStart(span)
        val end = spanned.getSpanEnd(span)
        
        when (span) {
            is android.text.style.StyleSpan -> {
                when (span.style) {
                    android.graphics.Typeface.BOLD -> {
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                    }
                    android.graphics.Typeface.ITALIC -> {
                        addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    }
                    android.graphics.Typeface.BOLD_ITALIC -> {
                        addStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic
                            ),
                            start,
                            end
                        )
                    }
                }
            }
            is android.text.style.UnderlineSpan -> {
                addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
            }
            is android.text.style.StrikethroughSpan -> {
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
            }
        }
    }
}

