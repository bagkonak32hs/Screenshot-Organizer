package com.example.data.model

import com.squareup.moshi.JsonClass
import org.json.JSONObject

@JsonClass(generateAdapter = true)
data class AIAnalysisDto(
    val category: String = "personal",
    val subcategory: String = "",
    val title: String = "",
    val summary: String = "",
    val confidence: Double = 0.85,
    val language: String = "tr",
    val detected_text: String = "",
    val important_fields: ImportantFieldsDto = ImportantFieldsDto(),
    val suggested_tags: List<String> = emptyList(),
    val suggested_categories: List<String> = emptyList(),
    val contains_sensitive_information: Boolean = false,
    val action_suggestions: List<String> = emptyList()
) {
    companion object {
        fun normalizeCategory(rawCategory: String): String {
            val normalized = rawCategory.trim().lowercase()
            return when {
                // 1. Fikirler & İlham (Ideas / Inspiration)
                normalized.contains("idea") || normalized.contains("ideas") ||
                        normalized.contains("fikir") || normalized.contains("ilham") ||
                        normalized.contains("inspiration") || normalized.contains("brainstorm") ||
                        normalized.contains("tasarim") || normalized.contains("tasarım") ||
                        normalized.contains("concept") || normalized.contains("moodboard") ||
                        normalized.contains("quote") || normalized.contains("yaratıcı") -> "ideas"

                // 2. İş (Work)
                normalized.contains("work") || normalized.contains("iş") ||
                        normalized.contains("is") || normalized.contains("calisma") ||
                        normalized.contains("çalışma") || normalized.contains("proje") ||
                        normalized.contains("office") || normalized.contains("jira") ||
                        normalized.contains("sprint") || normalized.contains("slack") ||
                        normalized.contains("teams") || normalized.contains("meeting") ||
                        normalized.contains("toplanti") || normalized.contains("toplantı") ||
                        normalized.contains("kurumsal") || normalized.contains("kod") ||
                        normalized.contains("code") || normalized.contains("github") -> "work"

                // 3. Kişisel (Personal)
                normalized.contains("personal") || normalized.contains("kisisel") ||
                        normalized.contains("kişisel") || normalized.contains("private") ||
                        normalized.contains("özel") || normalized.contains("ozel") ||
                        normalized.contains("aile") || normalized.contains("family") ||
                        normalized.contains("saglik") || normalized.contains("sağlık") ||
                        normalized.contains("randevu") || normalized.contains("hastane") ||
                        normalized.contains("doktor") || normalized.contains("gunluk") ||
                        normalized.contains("günlük") -> "personal"

                // 4. Okul (School)
                normalized.contains("school") || normalized.contains("okul") ||
                        normalized.contains("ders") || normalized.contains("sinav") ||
                        normalized.contains("sınav") || normalized.contains("odev") ||
                        normalized.contains("ödev") || normalized.contains("akadem") ||
                        normalized.contains("universite") || normalized.contains("üniversite") ||
                        normalized.contains("vize") || normalized.contains("final") ||
                        normalized.contains("lecture") || normalized.contains("homework") ||
                        normalized.contains("study") || normalized.contains("slayt") -> "school"

                // 5. Eğlence (Entertainment)
                normalized.contains("entertainment") || normalized.contains("eglence") ||
                        normalized.contains("eğlence") || normalized.contains("oyun") ||
                        normalized.contains("game") || normalized.contains("gaming") ||
                        normalized.contains("steam") || normalized.contains("film") ||
                        normalized.contains("movie") || normalized.contains("netflix") ||
                        normalized.contains("dizi") || normalized.contains("youtube") ||
                        normalized.contains("spotify") || normalized.contains("muzik") ||
                        normalized.contains("müzik") || normalized.contains("music") ||
                        normalized.contains("meme") || normalized.contains("mizah") ||
                        normalized.contains("twitch") || normalized.contains("tiktok") ||
                        normalized.contains("cinema") || normalized.contains("sinema") -> "entertainment"

                // Supporting secondary types mapped cleanly
                normalized.contains("finance") || normalized.contains("finans") || normalized.contains("bank") || normalized.contains("dekont") -> "finance"
                normalized.contains("invoice") || normalized.contains("fatura") || normalized.contains("bill") -> "invoice"
                normalized.contains("receipt") || normalized.contains("makbuz") || normalized.contains("fiş") || normalized.contains("fis") -> "receipt"
                normalized.contains("address") || normalized.contains("adres") || normalized.contains("location") || normalized.contains("harita") -> "address"
                normalized.contains("chat") || normalized.contains("konusma") || normalized.contains("sohbet") || normalized.contains("whatsapp") -> "chat"
                normalized.contains("product") || normalized.contains("urun") || normalized.contains("ürün") || normalized.contains("shopping") || normalized.contains("sepet") -> "product"
                normalized.contains("travel") || normalized.contains("seyahat") || normalized.contains("ucak") || normalized.contains("flight") -> "travel"
                normalized.contains("reservation") || normalized.contains("rezervasyon") || normalized.contains("hotel") || normalized.contains("otel") -> "reservation"
                normalized.contains("recipe") || normalized.contains("tarif") || normalized.contains("yemek") || normalized.contains("food") -> "recipe"
                normalized.contains("note") || normalized.contains("not") -> "note"
                normalized.contains("phone") || normalized.contains("telefon") -> "phone"
                normalized.contains("social") || normalized.contains("sosyal") || normalized.contains("instagram") || normalized.contains("tweet") -> "social"
                normalized.contains("document") || normalized.contains("belge") || normalized.contains("evrak") -> "document"
                else -> "personal"
            }
        }

        fun fromJson(jsonStr: String): AIAnalysisDto {
            return try {
                // Strip markdown backticks if present
                val cleaned = jsonStr
                    .trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val root = JSONObject(cleaned)
                val rawCategory = root.optString("category", "personal")
                val category = normalizeCategory(rawCategory)

                val subcategory = root.optString("subcategory", "")
                val title = root.optString("title", "Ekran Görüntüsü")
                val summary = root.optString("summary", "")
                val confidence = root.optDouble("confidence", 0.92)
                val language = root.optString("language", "tr")
                val detectedText = root.optString("detected_text", "")

                val fieldsObj = root.optJSONObject("important_fields")
                val fields = if (fieldsObj != null) {
                    ImportantFieldsDto(
                        company = fieldsObj.optString("company").takeIf { it.isNotEmpty() },
                        amount = fieldsObj.optString("amount").takeIf { it.isNotEmpty() },
                        date = fieldsObj.optString("date").takeIf { it.isNotEmpty() },
                        invoice_number = fieldsObj.optString("invoice_number").takeIf { it.isNotEmpty() },
                        address = fieldsObj.optString("address").takeIf { it.isNotEmpty() },
                        phone_number = fieldsObj.optString("phone_number").takeIf { it.isNotEmpty() },
                        order_number = fieldsObj.optString("order_number").takeIf { it.isNotEmpty() },
                        tracking_number = fieldsObj.optString("tracking_number").takeIf { it.isNotEmpty() }
                    )
                } else {
                    ImportantFieldsDto()
                }

                val tagsList = mutableListOf<String>()
                val tagsArr = root.optJSONArray("suggested_tags")
                if (tagsArr != null) {
                    for (i in 0 until tagsArr.length()) {
                        val t = tagsArr.getString(i).trim().removePrefix("#")
                        if (t.isNotEmpty()) {
                            tagsList.add(t)
                        }
                    }
                }

                // If tags are empty, add category default tags
                if (tagsList.isEmpty()) {
                    when (category) {
                        "ideas" -> tagsList.addAll(listOf("Fikirler", "İlham", "Ideas", "Tasarım", "Konsept"))
                        "work" -> tagsList.addAll(listOf("İş", "Work", "Proje", "Toplantı", "Görev"))
                        "personal" -> tagsList.addAll(listOf("Kişisel", "Personal", "Özel", "Not", "Sohbet"))
                        "school" -> tagsList.addAll(listOf("Okul", "Ders", "Sınav", "Ödev", "Akademik"))
                        "entertainment" -> tagsList.addAll(listOf("Eğlence", "Oyun", "Film", "Müzik", "Medya"))
                        "finance" -> tagsList.addAll(listOf("Finance", "Banka", "Dekont", "Finans"))
                        "invoice" -> tagsList.addAll(listOf("Fatura", "Invoice", "Ödeme"))
                        "receipt" -> tagsList.addAll(listOf("Makbuz", "Fiş", "Receipt"))
                        "address" -> tagsList.addAll(listOf("Adres", "Address", "Konum"))
                        "chat" -> tagsList.addAll(listOf("Chat", "Konuşma", "Mesaj"))
                        "product" -> tagsList.addAll(listOf("Product", "Ürün", "Alışveriş"))
                        "travel" -> tagsList.addAll(listOf("Travel", "Seyahat", "Bilet"))
                        "reservation" -> tagsList.addAll(listOf("Reservation", "Rezervasyon", "Otel"))
                        "recipe" -> tagsList.addAll(listOf("Recipe", "Tarif", "Yemek"))
                        else -> tagsList.addAll(listOf("Kişisel", "Screenshot", "Arşiv"))
                    }
                }

                // Parse suggested_categories (like Work, Personal, Ideas)
                val suggestedCategoriesList = mutableListOf<String>()
                val categoriesArr = root.optJSONArray("suggested_categories")
                if (categoriesArr != null) {
                    for (i in 0 until categoriesArr.length()) {
                        val cat = categoriesArr.getString(i).trim()
                        if (cat.isNotEmpty()) {
                            suggestedCategoriesList.add(cat)
                        }
                    }
                }

                if (suggestedCategoriesList.isEmpty()) {
                    when (category) {
                        "work" -> suggestedCategoriesList.addAll(listOf("Work", "Ideas", "Personal"))
                        "ideas" -> suggestedCategoriesList.addAll(listOf("Ideas", "Work", "Personal"))
                        "personal" -> suggestedCategoriesList.addAll(listOf("Personal", "Ideas", "Work"))
                        "school" -> suggestedCategoriesList.addAll(listOf("School", "Work", "Ideas"))
                        "entertainment" -> suggestedCategoriesList.addAll(listOf("Entertainment", "Ideas", "Personal"))
                        else -> suggestedCategoriesList.addAll(listOf("Personal", "Work", "Ideas"))
                    }
                }

                val sensitive = root.optBoolean("contains_sensitive_information", false)

                val actionsList = mutableListOf<String>()
                val actionsArr = root.optJSONArray("action_suggestions")
                if (actionsArr != null) {
                    for (i in 0 until actionsArr.length()) {
                        actionsList.add(actionsArr.getString(i))
                    }
                }

                AIAnalysisDto(
                    category = category,
                    subcategory = subcategory,
                    title = title,
                    summary = summary,
                    confidence = confidence,
                    language = language,
                    detected_text = detectedText,
                    important_fields = fields,
                    suggested_tags = tagsList,
                    suggested_categories = suggestedCategoriesList,
                    contains_sensitive_information = sensitive,
                    action_suggestions = actionsList
                )
            } catch (e: Exception) {
                AIAnalysisDto(
                    category = "personal",
                    title = "Analiz Edilen Belge",
                    summary = "Yapay zekâ içeriği işledi.",
                    confidence = 0.75,
                    detected_text = jsonStr.take(500),
                    suggested_tags = listOf("Kişisel", "Screenshot", "Arşiv"),
                    suggested_categories = listOf("Work", "Personal", "Ideas")
                )
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class ImportantFieldsDto(
    val company: String? = null,
    val amount: String? = null,
    val date: String? = null,
    val invoice_number: String? = null,
    val address: String? = null,
    val phone_number: String? = null,
    val order_number: String? = null,
    val tracking_number: String? = null
)
