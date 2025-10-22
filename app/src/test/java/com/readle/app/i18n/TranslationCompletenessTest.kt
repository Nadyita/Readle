package com.readle.app.i18n

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class TranslationCompletenessTest {

    private val projectRoot = File("").absoluteFile.parentFile
    private val resDir = File(projectRoot, "app/src/main/res")
    
    @Test
    fun `all translations should have the same string keys`() {
        val defaultStrings = parseStringsXml(File(resDir, "values/strings.xml"))
        val languages = findTranslationFolders()
        
        assertTrue("No translation folders found", languages.isNotEmpty())
        
        val errors = mutableListOf<String>()
        
        languages.forEach { (language, file) ->
            val translatedStrings = parseStringsXml(file)
            
            // Check for missing translations
            val missingKeys = defaultStrings.keys - translatedStrings.keys
            if (missingKeys.isNotEmpty()) {
                errors.add("Missing translations in $language: ${missingKeys.joinToString(", ")}")
            }
            
            // Check for extra translations
            val extraKeys = translatedStrings.keys - defaultStrings.keys
            if (extraKeys.isNotEmpty()) {
                errors.add("Extra translations in $language: ${extraKeys.joinToString(", ")}")
            }
        }
        
        if (errors.isNotEmpty()) {
            fail("Translation completeness check failed:\n" + errors.joinToString("\n"))
        }
    }
    
    @Test
    fun `translated strings should have matching placeholders`() {
        val defaultStrings = parseStringsXml(File(resDir, "values/strings.xml"))
        val languages = findTranslationFolders()
        
        val errors = mutableListOf<String>()
        
        languages.forEach { (language, file) ->
            val translatedStrings = parseStringsXml(file)
            
            translatedStrings.forEach { (key, translatedValue) ->
                val defaultValue = defaultStrings[key]
                if (defaultValue == null) return@forEach
                
                val defaultPlaceholders = extractPlaceholders(defaultValue)
                val translatedPlaceholders = extractPlaceholders(translatedValue)
                
                if (defaultPlaceholders != translatedPlaceholders) {
                    errors.add(
                        "Placeholder mismatch in $language for key '$key':\n" +
                        "  Default:    $defaultPlaceholders\n" +
                        "  Translated: $translatedPlaceholders"
                    )
                }
            }
        }
        
        if (errors.isNotEmpty()) {
            fail("Placeholder check failed:\n" + errors.joinToString("\n"))
        }
    }
    
    @Test
    fun `all translation files should be valid XML`() {
        val errors = mutableListOf<String>()
        
        findTranslationFolders().forEach { (language, file) ->
            try {
                parseStringsXml(file)
            } catch (e: Exception) {
                errors.add("Invalid XML in $language: ${e.message}")
            }
        }
        
        // Also check default
        try {
            parseStringsXml(File(resDir, "values/strings.xml"))
        } catch (e: Exception) {
            errors.add("Invalid XML in default (en): ${e.message}")
        }
        
        if (errors.isNotEmpty()) {
            fail("XML validation failed:\n" + errors.joinToString("\n"))
        }
    }
    
    private fun findTranslationFolders(): Map<String, File> {
        val translations = mutableMapOf<String, File>()
        
        resDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory && dir.name.startsWith("values-")) {
                val languageCode = dir.name.removePrefix("values-")
                val stringsFile = File(dir, "strings.xml")
                if (stringsFile.exists()) {
                    translations[languageCode] = stringsFile
                }
            }
        }
        
        return translations
    }
    
    private fun parseStringsXml(file: File): Map<String, String> {
        val strings = mutableMapOf<String, String>()
        
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc: Document = builder.parse(file)
        
        val stringElements = doc.getElementsByTagName("string")
        for (i in 0 until stringElements.length) {
            val element = stringElements.item(i) as Element
            val name = element.getAttribute("name")
            val value = element.textContent
            if (name.isNotEmpty()) {
                strings[name] = value
            }
        }
        
        return strings
    }
    
    private fun extractPlaceholders(text: String): Set<String> {
        val placeholderRegex = Regex("""%([\d$]*[sdxf]|%)""")
        return placeholderRegex.findAll(text).map { it.value }.toSet()
    }
}

