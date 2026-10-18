package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "screenshots",
    indices = [
        Index("categoryId"),
        Index("createdAt"),
        Index("isFavorite"),
        Index("isHidden")
    ]
)
data class ScreenshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val fileName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val categoryId: String = CATEGORY_PERSONAL,
    val subcategory: String = "",
    val title: String = "",
    val summary: String = "",
    val ocrText: String = "",
    val confidence: Float = 0f,
    val isFavorite: Boolean = false,
    val isSensitive: Boolean = false,
    val isHidden: Boolean = false,
    val isAnalyzed: Boolean = false,
    val analysisStatus: String = "PENDING", // PENDING, ANALYZING, COMPLETED, FAILED
    
    // Extracted important fields
    val company: String? = null,
    val amount: String? = null,
    val dateStr: String? = null,
    val invoiceNumber: String? = null,
    val address: String? = null,
    val phoneNumber: String? = null,
    val orderNumber: String? = null,
    val trackingNumber: String? = null,
    
    val tagsJson: String = "", // Comma-separated or JSON list
    val suggestedCategoriesJson: String = "", // AI suggested categories, e.g. "Work, Ideas, Personal"
    val actionSuggestion: String? = null,
    val rawJson: String? = null,
    val isDemo: Boolean = false,
    val userCorrection: String? = null, // e.g. "incorrect_category_flag"
    val userNotes: String? = null
) {
    val isWorkCategory: Boolean get() = categoryId == CATEGORY_WORK
    val isPersonalCategory: Boolean get() = categoryId == CATEGORY_PERSONAL
    val isIdeasCategory: Boolean get() = categoryId == CATEGORY_IDEAS
    val isSchoolCategory: Boolean get() = categoryId == CATEGORY_SCHOOL
    val isEntertainmentCategory: Boolean get() = categoryId == CATEGORY_ENTERTAINMENT

    val categoryDisplayName: String
        get() = when (categoryId) {
            CATEGORY_WORK -> "İş / Work"
            CATEGORY_PERSONAL -> "Kişisel / Personal"
            CATEGORY_IDEAS -> "Fikirler & İlham / Ideas"
            CATEGORY_SCHOOL -> "Okul / School"
            CATEGORY_ENTERTAINMENT -> "Eğlence / Entertainment"
            "invoice" -> "Faturalar"
            "finance" -> "Banka & Finans"
            "chat" -> "Konuşmalar"
            "note" -> "Notlar"
            "address" -> "Adresler"
            "travel" -> "Seyahat"
            else -> "Diğer"
        }

    val tagsList: List<String>
        get() = if (tagsJson.isBlank()) emptyList() else tagsJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val suggestedCategoriesList: List<String>
        get() = if (suggestedCategoriesJson.isBlank()) {
            listOf("Work", "Personal", "Ideas")
        } else {
            suggestedCategoriesJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

    companion object {
        const val CATEGORY_WORK = "work"
        const val CATEGORY_PERSONAL = "personal"
        const val CATEGORY_IDEAS = "ideas"
        const val CATEGORY_SCHOOL = "school"
        const val CATEGORY_ENTERTAINMENT = "entertainment"
        const val CATEGORY_OTHER = "other"

        val CORE_CATEGORIES = listOf(
            CATEGORY_WORK,
            CATEGORY_PERSONAL,
            CATEGORY_IDEAS,
            CATEGORY_SCHOOL,
            CATEGORY_ENTERTAINMENT
        )
    }
}
