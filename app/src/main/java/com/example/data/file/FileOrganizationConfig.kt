package com.example.data.file

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ScreenshotEntity
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileOrganizationConfig(
    val namingPattern: String = "{category}_{date}_{title}",
    val folderStructurePattern: String = "{category}/{year_month}",
    val dateFormat: String = "yyyy-MM-dd",
    val lowercaseNames: Boolean = false,
    val spaceReplacement: String = "_", // "_", "-", or " "
    val customCategoryFolderNames: Map<String, String> = emptyMap()
) {
    companion object {
        val PRESET_NAMING_PATTERNS = listOf(
            "{category}_{date}_{title}" to "Standart (Kategori_Tarih_Başlık)",
            "{company}_{category}_{amount}_{date}" to "Detaylı (Kurum_Kategori_Tutar_Tarih)",
            "{date}_{category}_{title}" to "Kronolojik (Tarih_Kategori_Başlık)",
            "{category}_{date}" to "Sade (Kategori_Tarih)",
            "{original}" to "Orijinal Dosya Adı"
        )

        val PRESET_FOLDER_STRUCTURES = listOf(
            "{category}" to "Yalnızca Kategori (/Finance/)",
            "{category}/{year_month}" to "Kategori / Yıl-Ay (/Finance/2026-10/)",
            "{category}/{year}/{month}" to "Kategori / Yıl / Ay (/Finance/2026/10/)",
            "{year}/{category}" to "Yıl / Kategori (/2026/Finance/)",
            "{category}/{company}" to "Kategori / Kurum (/Finance/Garanti/)"
        )

        val AVAILABLE_TOKENS = listOf(
            "{category}" to "Kategori Adı",
            "{company}" to "Kurum / Firma",
            "{title}" to "Belge Başlığı",
            "{date}" to "Tarih",
            "{year}" to "Yıl (2026)",
            "{month}" to "Ay (10)",
            "{year_month}" to "Yıl-Ay (2026-10)",
            "{amount}" to "Tutar / Miktar",
            "{id}" to "Kayıt No",
            "{original}" to "Orijinal İsim"
        )

        fun getSampleScreenshot(): ScreenshotEntity {
            return ScreenshotEntity(
                id = 42,
                uri = "content://media/sample.jpg",
                fileName = "Screenshot_20261004_134012.png",
                createdAt = 1791120000000L, // Sample timestamp
                categoryId = "finance",
                title = "FAST Dekont",
                company = "Garanti BBVA",
                amount = "2450TL"
            )
        }
    }

    /**
     * Resolves custom folder path and custom file name for a given screenshot.
     * Returns: Pair(RelativeFolderPath, FinalFileNameWithExt)
     */
    fun resolvePathAndFileName(screenshot: ScreenshotEntity): Pair<String, String> {
        val dateObj = Date(if (screenshot.createdAt > 0) screenshot.createdAt else System.currentTimeMillis())
        val dateFormatted = try {
            SimpleDateFormat(dateFormat, Locale.getDefault()).format(dateObj)
        } catch (_: Exception) {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dateObj)
        }
        val yearFormatted = SimpleDateFormat("yyyy", Locale.getDefault()).format(dateObj)
        val monthFormatted = SimpleDateFormat("MM", Locale.getDefault()).format(dateObj)
        val yearMonthFormatted = "$yearFormatted-$monthFormatted"

        // Normalized category or user-defined override
        val baseCategory = ScreenshotFileService.normalizeCategoryName(screenshot.categoryId)
        val categoryFolderName = customCategoryFolderNames[screenshot.categoryId.lowercase()]
            ?: customCategoryFolderNames[baseCategory.lowercase()]
            ?: baseCategory

        val companyClean = sanitizeTokenValue(screenshot.company ?: "")
        val titleClean = sanitizeTokenValue(screenshot.title.ifBlank { "Belge_${screenshot.id}" })
        val amountClean = sanitizeTokenValue(screenshot.amount ?: "").replace(" ", "")
        val originalBase = screenshot.fileName.substringBeforeLast(".", "gorsel")
        val ext = screenshot.fileName.substringAfterLast(".", "jpg").lowercase()

        // 1. Resolve Folder Structure
        var folder = folderStructurePattern
            .replace("{category}", categoryFolderName)
            .replace("{company}", companyClean.ifBlank { "Genel" })
            .replace("{year}", yearFormatted)
            .replace("{month}", monthFormatted)
            .replace("{year_month}", yearMonthFormatted)
            .replace("{date}", dateFormatted)
            .replace("{title}", titleClean)

        // Clean folder separators
        folder = folder.split("/").filter { it.isNotBlank() }.joinToString("/")
        if (folder.isBlank()) {
            folder = categoryFolderName
        }

        // 2. Resolve File Name
        var resolvedName = namingPattern
            .replace("{category}", categoryFolderName)
            .replace("{company}", companyClean)
            .replace("{title}", titleClean)
            .replace("{date}", dateFormatted)
            .replace("{year}", yearFormatted)
            .replace("{month}", monthFormatted)
            .replace("{year_month}", yearMonthFormatted)
            .replace("{amount}", amountClean)
            .replace("{id}", screenshot.id.toString())
            .replace("{original}", originalBase)

        // Clean double separators (e.g. "__" -> "_") caused by missing tokens like company
        resolvedName = resolvedName
            .replace("_{2,}".toRegex(), "_")
            .replace("-{2,}".toRegex(), "-")
            .trim('_', '-', ' ')

        if (resolvedName.isBlank()) {
            resolvedName = "${categoryFolderName}_${dateFormatted}_${screenshot.id}"
        }

        // Apply space replacement
        if (spaceReplacement != " ") {
            resolvedName = resolvedName.replace(" ", spaceReplacement)
        }

        // Apply casing
        if (lowercaseNames) {
            resolvedName = resolvedName.lowercase(Locale.getDefault())
        }

        val finalFileName = "$resolvedName.$ext"
        return Pair(folder, finalFileName)
    }

    private fun sanitizeTokenValue(value: String): String {
        return value.trim()
            .replace("/", "-")
            .replace("\\", "-")
            .replace(":", "-")
            .replace("*", "")
            .replace("?", "")
            .replace("\"", "")
            .replace("<", "")
            .replace(">", "")
            .replace("|", "")
    }
}

class FileOrganizationPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("file_organization_prefs", Context.MODE_PRIVATE)

    fun loadConfig(): FileOrganizationConfig {
        val naming = prefs.getString("naming_pattern", "{category}_{date}_{title}") ?: "{category}_{date}_{title}"
        val folder = prefs.getString("folder_pattern", "{category}/{year_month}") ?: "{category}/{year_month}"
        val dateFormat = prefs.getString("date_format", "yyyy-MM-dd") ?: "yyyy-MM-dd"
        val lowercase = prefs.getBoolean("lowercase_names", false)
        val spaceRep = prefs.getString("space_replacement", "_") ?: "_"

        val customCatJson = prefs.getString("custom_categories_json", "{}") ?: "{}"
        val customMap = mutableMapOf<String, String>()
        try {
            val json = JSONObject(customCatJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                customMap[key] = json.getString(key)
            }
        } catch (_: Exception) {}

        return FileOrganizationConfig(
            namingPattern = naming,
            folderStructurePattern = folder,
            dateFormat = dateFormat,
            lowercaseNames = lowercase,
            spaceReplacement = spaceRep,
            customCategoryFolderNames = customMap
        )
    }

    fun saveConfig(config: FileOrganizationConfig) {
        val json = JSONObject()
        config.customCategoryFolderNames.forEach { (k, v) ->
            json.put(k, v)
        }

        prefs.edit()
            .putString("naming_pattern", config.namingPattern)
            .putString("folder_pattern", config.folderStructurePattern)
            .putString("date_format", config.dateFormat)
            .putBoolean("lowercase_names", config.lowercaseNames)
            .putString("space_replacement", config.spaceReplacement)
            .putString("custom_categories_json", json.toString())
            .apply()
    }
}
