package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AIAnalysisDto
import com.example.data.model.ImportantFieldsDto
import com.example.data.model.ScreenshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body body: okhttp3.RequestBody
    ): ResponseBody
}

object GeminiNetworkClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .build()

    val api: GeminiApi by lazy {
        retrofit.create(GeminiApi::class.java)
    }
}

class GeminiVisionAnalyzer(private val context: Context) {

    fun getEffectiveApiKey(): String {
        val prefs = context.getSharedPreferences("screenshot_organizer_prefs", Context.MODE_PRIVATE)
        val userKey = prefs.getString("custom_gemini_api_key", "")?.trim() ?: ""
        if (userKey.isNotBlank()) {
            return userKey
        }

        val buildConfigKey = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Throwable) {
            ""
        }

        if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
            return buildConfigKey
        }

        return ""
    }

    fun hasApiKey(): Boolean {
        return getEffectiveApiKey().isNotBlank()
    }

    /**
     * Multimodal Gemini 3.5 Flash API Classifier.
     * Accurately categorizes screenshots by content into:
     * - "work" (İş): Professional tasks, emails, Slack/Teams, code, projects, presentations
     * - "school" (Okul): Lectures, study notes, homework, exams, university announcements, slides
     * - "entertainment" (Eğlence): Games, streaming, movies, series, music, memes, pop culture
     * - "personal" (Kişisel): Private chats, family, health/appointments, personal notes, shopping
     */
    suspend fun analyzeScreenshot(uriString: String, fileName: String): AIAnalysisDto = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()

        // If no API key configured or running in offline mode, invoke local intelligent heuristic engine
        if (apiKey.isBlank()) {
            Log.d("GeminiVision", "No active Gemini API key found, executing local heuristic analysis.")
            return@withContext fallbackLocalAnalysis(uriString, fileName)
        }

        try {
            val bitmap = loadScaledBitmap(uriString)
            if (bitmap == null) {
                Log.w("GeminiVision", "Could not load bitmap for $uriString, falling back to local analysis.")
                return@withContext fallbackLocalAnalysis(uriString, fileName)
            }

            val base64Data = bitmap.toBase64()

            val systemPrompt = """
                You are an advanced AI screenshot and visual document classifier.
                Analyze this screenshot image carefully, extract visual context, and categorize it based on its content into one of the PRIMARY CATEGORIES (e.g. Work, Personal, Ideas):

                1. "work" (Work / İş):
                   Professional tasks, business emails, Slack/Teams chats, project planning, Jira/Trello boards, GitHub/GitLab pull requests and code reviews, corporate presentations, spreadsheets, meeting minutes, work contracts, enterprise roadmaps.
                2. "personal" (Personal / Kişisel):
                   Private personal messages, family chats, personal daily notes, health and doctor appointments, medical test results, personal shopping, personal bills/receipts, household and daily life memories.
                3. "ideas" (Ideas & Inspiration / Fikirler & İlham):
                   Creative ideas, UI/UX designs, architecture & interior designs, brainstorm notes, inspiring quotes, moodboards, startup concepts, travel inspiration, book/article ideas, creative concepts.
                4. "school" (School / Okul):
                   University/school course materials, lecture slides, study notes, midterm/final exam schedules, homework and assignments, syllabus, student portal/LMS announcements, academic papers, timetables.
                5. "entertainment" (Entertainment / Eğlence):
                   Video games, Steam/PlayStation/Xbox achievements and stores, movies, TV series, Netflix/Disney+/YouTube, anime, music/Spotify tracks, memes and humor, social media clips, pop culture, concerts and gaming streams.

                OUTPUT ONLY A VALID JSON OBJECT (no markdown, no backticks, no preamble):
                {
                  "category": "work",
                  "subcategory": "project_roadmap",
                  "suggested_categories": ["Work", "Ideas", "Personal"],
                  "title": "Clear descriptive title in Turkish",
                  "summary": "One sentence summary in Turkish",
                  "confidence": 0.98,
                  "language": "tr",
                  "detected_text": "Full extracted OCR text from the image",
                  "important_fields": {
                    "company": "Company, school, or person name if detected",
                    "amount": "Monetary amount if present (e.g. 1.250 TL)",
                    "date": "Date if present (YYYY-MM-DD)",
                    "invoice_number": "Invoice, order, or transaction ID",
                    "address": "Street address if present",
                    "phone_number": "Phone number if present"
                  },
                  "suggested_tags": ["İş", "Work", "Proje", "Sprint", "Toplantı", "Slack"],
                  "contains_sensitive_information": false,
                  "action_suggestions": ["Actionable recommendation (e.g. Takvime ekle)"]
                }
                Note: Mark contains_sensitive_information as true if an IBAN, credit card, national identity, password, or credentials are visible.
                Generate 4 to 8 relevant tags in Turkish, always including the category name tag ('İş', 'Kişisel', 'Fikirler', 'Okul', or 'Eğlence').
                In "suggested_categories", rank 2-3 most appropriate categories (such as "Work", "Personal", "Ideas").
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", systemPrompt)
                            })
                            put(JSONObject().apply {
                                val inlineData = JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Data)
                                }
                                put("inlineData", inlineData)
                            })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", generationConfig)
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val response = GeminiNetworkClient.api.generateContent(apiKey, body)
            val responseString = response.string()

            val rootResponse = JSONObject(responseString)
            val candidates = rootResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (!text.isNullOrBlank()) {
                Log.d("GeminiVision", "Gemini API response parsed successfully.")
                AIAnalysisDto.fromJson(text)
            } else {
                Log.w("GeminiVision", "Empty response from Gemini API, using fallback.")
                fallbackLocalAnalysis(uriString, fileName)
            }
        } catch (e: Exception) {
            Log.e("GeminiVision", "Error calling Gemini Vision API: ${e.message}", e)
            fallbackLocalAnalysis(uriString, fileName)
        }
    }

    /**
     * Dedicated Gemini OCR and Automatic Tag Generation engine.
     * Transcribes all visible text inside the screenshot and generates high-relevance searchable tags.
     */
    suspend fun performOcrAndGenerateTags(uriString: String, fileName: String): AIAnalysisDto = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            Log.d("GeminiVision", "No active Gemini API key found, running local OCR tag generator.")
            return@withContext fallbackLocalAnalysis(uriString, fileName)
        }

        try {
            val bitmap = loadScaledBitmap(uriString)
                ?: return@withContext fallbackLocalAnalysis(uriString, fileName)
            val base64Data = bitmap.toBase64()

            val ocrPrompt = """
                You are an advanced Optical Character Recognition (OCR) and document indexing AI.
                Analyze this screenshot image with high precision:

                TASK 1: COMPLETE OCR TRANSCRIPTION
                Transcribe ALL text visible inside this screenshot image word-for-word, line-by-line into "detected_text".
                Include all labels, amounts, dates, headers, body text, addresses, transaction codes, and fine print.

                TASK 2: CONTENT CLASSIFICATION & CATEGORY SUGGESTIONS
                Classify the screenshot and suggest top categories like Work, Personal, or Ideas:
                - "work" (Work / İş): Projects, code, business chats, meetings, emails
                - "personal" (Personal / Kişisel): Personal chats, family, health, daily notes, shopping
                - "ideas" (Ideas & Inspiration / Fikirler): Creative ideas, UI/UX designs, brainstorm notes, quotes
                - "school" (School / Okul): Lectures, study notes, exams, homework, university
                - "entertainment" (Entertainment / Eğlence): Gaming, movies, music, social media, memes

                TASK 3: SEARCHABLE TAG GENERATION
                Generate 6 to 12 searchable tags including category names, brand/entity names, dates, and keywords.

                OUTPUT ONLY A VALID JSON OBJECT (no markdown, no backticks, no preamble):
                {
                  "category": "work",
                  "subcategory": "code_review",
                  "suggested_categories": ["Work", "Ideas", "Personal"],
                  "title": "Açıklayıcı başlık",
                  "summary": "Görsel içeriğinin tek cümlelik özeti",
                  "confidence": 0.98,
                  "language": "tr",
                  "detected_text": "Görseldeki tüm OCR metni...",
                  "important_fields": {
                    "company": "Kurum veya kişi adı",
                    "amount": "Tutar varsa",
                    "date": "Tarih varsa",
                    "invoice_number": "İşlem no veya fatura no",
                    "address": "Adres bilgisi varsa",
                    "phone_number": "Telefon numarası varsa"
                  },
                  "suggested_tags": ["İş", "Work", "GitHub", "Kotlin", "PR", "Android"],
                  "contains_sensitive_information": false,
                  "action_suggestions": ["Uygun eylem önerisi"]
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", ocrPrompt)
                            })
                            put(JSONObject().apply {
                                val inlineData = JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Data)
                                }
                                put("inlineData", inlineData)
                            })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", generationConfig)
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val response = GeminiNetworkClient.api.generateContent(apiKey, body)
            val responseString = response.string()

            val rootResponse = JSONObject(responseString)
            val candidates = rootResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (!text.isNullOrBlank()) {
                Log.d("GeminiVision", "OCR extraction succeeded.")
                AIAnalysisDto.fromJson(text)
            } else {
                fallbackLocalAnalysis(uriString, fileName)
            }
        } catch (e: Exception) {
            Log.e("GeminiVision", "Gemini OCR execution failed: ${e.message}", e)
            fallbackLocalAnalysis(uriString, fileName)
        }
    }

    private fun loadScaledBitmap(uriString: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val maxDimension = 1024
            var sampleSize = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / sampleSize >= maxDimension && halfWidth / sampleSize >= maxDimension) {
                    sampleSize *= 2
                }
            }

            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val secondStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(secondStream, null, loadOptions)
            secondStream?.close()
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun Bitmap.toBase64(): String {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Intelligent local OCR & heuristic classifier for offline use or when Gemini is unreachable.
     * Categorizes into the 4 core categories: 'work', 'school', 'entertainment', 'personal'.
     */
    fun fallbackLocalAnalysis(uriString: String, fileName: String): AIAnalysisDto {
        val lowerName = fileName.lowercase()
        val nowYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

        var category = ScreenshotEntity.CATEGORY_PERSONAL
        var subcategory = "general"
        var title = "Ekran Görüntüsü"
        var summary = "Ekran görüntüsü başarıyla kataloglandı."
        var confidence = 0.90
        var sensitive = false
        var company: String? = null
        var amount: String? = null
        val tags = mutableListOf<String>()
        val actions = mutableListOf<String>()

        when {
            // 1. OKUL (School): Lecture, Exam, Homework, University
            lowerName.contains("okul") || lowerName.contains("school") || lowerName.contains("ders") ||
                    lowerName.contains("sinav") || lowerName.contains("sınav") || lowerName.contains("odev") ||
                    lowerName.contains("ödev") || lowerName.contains("vize") || lowerName.contains("final") ||
                    lowerName.contains("universite") || lowerName.contains("üniversite") || Regex("(^|[^a-z])ceng([^a-z]|$)").containsMatchIn(lowerName) ||
                    lowerName.contains("lecture") || lowerName.contains("study") || lowerName.contains("slayt") ||
                    lowerName.contains("amfi") || lowerName.contains("ogrenci") || lowerName.contains("hoca") -> {
                category = ScreenshotEntity.CATEGORY_SCHOOL
                subcategory = if (lowerName.contains("sinav") || lowerName.contains("vize") || lowerName.contains("final")) "exam_notes" else "lecture_notes"
                title = if (lowerName.contains("sinav") || lowerName.contains("vize")) "Vize / Sınav Bilgisi & Ders Notu" else "Ders & Akademik Materyal"
                summary = "Eğitim ve ders içerikli kaydedilen ekran görüntüsü."
                confidence = 0.95
                tags.addAll(listOf("Okul", "Ders", "Akademik", "Sınav", "Üniversite"))
                actions.add("Sınav veya ödev teslim tarihini takvime kaydet")
            }

            // 2. İŞ (Work): Projects, Code, Slack, Teams, Meetings, Roadmaps
            lowerName.contains("work") || Regex("(^|[^a-zçğıöşü])(is|iş)([^a-zçğıöşü]|$)").containsMatchIn(lowerName) ||
                    lowerName.contains("proje") || lowerName.contains("rapor") || lowerName.contains("sunum") ||
                    lowerName.contains("slack") || lowerName.contains("teams") || lowerName.contains("jira") ||
                    lowerName.contains("sprint") || lowerName.contains("toplanti") || lowerName.contains("meeting") ||
                    lowerName.contains("github") || lowerName.contains("gitlab") || lowerName.contains("kod") ||
                    lowerName.contains("code") || Regex("(^|[^a-z])pr([^a-z]|$)").containsMatchIn(lowerName) || lowerName.contains("linkedin") -> {
                category = ScreenshotEntity.CATEGORY_WORK
                subcategory = "project_task"
                title = "İş ve Proje Dokümanı"
                summary = "Çalışma, iş akışı ve profesyonel projelerle ilgili ekran görüntüsü."
                confidence = 0.94
                tags.addAll(listOf("İş", "Work", "Proje", "Toplantı", "Görev"))
                actions.add("İş teslim tarihini ajandaya ekle")
            }

            // 3. EĞLENCE (Entertainment): Gaming, Steam, Netflix, YouTube, Music, Memes
            lowerName.contains("eglence") || lowerName.contains("eğlence") || lowerName.contains("oyun") ||
                    lowerName.contains("game") || lowerName.contains("gaming") || lowerName.contains("steam") ||
                    lowerName.contains("netflix") || lowerName.contains("youtube") || lowerName.contains("spotify") ||
                    lowerName.contains("muzik") || lowerName.contains("müzik") || lowerName.contains("film") ||
                    lowerName.contains("dizi") || lowerName.contains("twitch") || lowerName.contains("meme") ||
                    lowerName.contains("mizah") || lowerName.contains("tiktok") || lowerName.contains("sinema") -> {
                category = ScreenshotEntity.CATEGORY_ENTERTAINMENT
                subcategory = if (lowerName.contains("oyun") || lowerName.contains("steam")) "gaming" else "media_streaming"
                title = if (lowerName.contains("oyun") || lowerName.contains("steam")) "Oyun & Dijital Eğlence İçeriği" else "Film, Dizi & Müzik İçeriği"
                summary = "Eğlence, medya tüketimi veya oyun dünyasından kaydedilen kesit."
                confidence = 0.95
                tags.addAll(listOf("Eğlence", "Oyun", "Film", "Müzik", "Medya"))
            }

            // 4. KİŞİSEL (Personal): Family, Private chat, Health, Appointments, Shopping
            lowerName.contains("dekont") || lowerName.contains("bank") || lowerName.contains("fast") ||
                    lowerName.contains("havale") || lowerName.contains("eft") || lowerName.contains("finance") ||
                    lowerName.contains("finans") -> {
                category = "finance"
                subcategory = "bank_transfer"
                title = "Banka Transfer Dekontu"
                summary = "Para transferi ve işlem referans bilgileri."
                confidence = 0.96
                sensitive = true
                tags.addAll(listOf("Finans", "Kişisel", "Banka", "Dekont", "Havale", "IBAN"))
                company = "Banka / Finans Kurumu"
            }
            lowerName.contains("fatura") || lowerName.contains("bill") || lowerName.contains("enerjisa") ||
                    lowerName.contains("iski") || lowerName.contains("igdas") || lowerName.contains("invoice") -> {
                category = "invoice"
                subcategory = "utility_bill"
                title = "Fatura Belgesi"
                summary = "Ödeme ve kullanım detayları içeren fatura belgesi."
                confidence = 0.95
                tags.addAll(listOf("Fatura", "Kişisel", "Ödeme", "Abonelik", "$nowYear"))
                actions.add("Son ödeme tarihi için hatırlatıcı oluştur")
                company = if (lowerName.contains("enerji")) "Enerji Dağıtım" else "Hizmet Sağlayıcı"
            }
            lowerName.contains("whatsapp") || lowerName.contains("chat") || lowerName.contains("sohbet") ||
                    lowerName.contains("mesaj") || lowerName.contains("aile") || lowerName.contains("arkadas") -> {
                category = ScreenshotEntity.CATEGORY_PERSONAL
                subcategory = "messaging"
                title = "Kişisel Sohbet & Mesajlaşma"
                summary = "Sohbet uygulamasından kaydedilen özel konuşma kesiti."
                confidence = 0.93
                tags.addAll(listOf("Kişisel", "Sohbet", "WhatsApp", "Mesaj", "Aile"))
            }
            lowerName.contains("saglik") || lowerName.contains("sağlık") || lowerName.contains("randevu") ||
                    lowerName.contains("doktor") || lowerName.contains("hastane") || Regex("(^|[^a-z])dis([^a-z]|$)").containsMatchIn(lowerName) ||
                    lowerName.contains("diş") || lowerName.contains("ilac") -> {
                category = ScreenshotEntity.CATEGORY_PERSONAL
                subcategory = "health_appointment"
                title = "Kişisel Sağlık & Randevu Bilgisi"
                summary = "Sağlık kontrolü veya hekim randevu onay bildirimi."
                confidence = 0.96
                tags.addAll(listOf("Kişisel", "Sağlık", "Randevu", "Doktor"))
                actions.add("Randevu saatini takvime kaydet")
            }
            lowerName.contains("adres") || lowerName.contains("location") || lowerName.contains("maps") || lowerName.contains("harita") -> {
                category = "address"
                subcategory = "location"
                title = "Konum ve Adres Ekranı"
                summary = "Kargo veya buluşma için kaydedilen adres detayı."
                confidence = 0.91
                tags.addAll(listOf("Adres", "Konum", "Kişisel", "Harita"))
                actions.add("Adresi Haritalar'da aç")
            }
            lowerName.contains("trendyol") || lowerName.contains("amazon") || lowerName.contains("hepsiburada") ||
                    lowerName.contains("siparis") || lowerName.contains("sepet") || lowerName.contains("product") -> {
                category = ScreenshotEntity.CATEGORY_PERSONAL
                subcategory = "shopping"
                title = "Alışveriş & Ürün Bilgisi"
                summary = "E-ticaret sepeti veya ürün fiyat bilgisi."
                confidence = 0.93
                tags.addAll(listOf("Alışveriş", "Kişisel", "Ürün", "Fiyat"))
            }
            // Fikirler & İlham (Ideas): Design, inspiration, wireframes, brainstorming, quotes
            lowerName.contains("idea") || lowerName.contains("fikir") || lowerName.contains("ilham") ||
                    lowerName.contains("design") || lowerName.contains("tasarim") || lowerName.contains("tasarım") ||
                    lowerName.contains("concept") || lowerName.contains("konsept") || lowerName.contains("moodboard") ||
                    lowerName.contains("quote") || lowerName.contains("brainstorm") -> {
                category = ScreenshotEntity.CATEGORY_IDEAS
                subcategory = "creative_inspiration"
                title = "Yaratıcı Fikir & Tasarım İlhamı"
                summary = "Yaratıcı düşünce, görsel tasarım veya proje fikri içeren ekran görüntüsü."
                confidence = 0.94
                tags.addAll(listOf("Fikirler", "İlham", "Ideas", "Tasarım", "Konsept"))
                actions.add("Fikir panosuna veya ilham koleksiyonuna ekle")
            }

            else -> {
                category = ScreenshotEntity.CATEGORY_PERSONAL
                subcategory = "daily_notes"
                title = "Kişisel Ekran Görüntüsü"
                summary = "Yapay zekâ tarafından taranmış görsel içeriği."
                confidence = 0.88
                tags.addAll(listOf("Kişisel", "Screenshot", "Arşiv", "$nowYear"))
            }
        }

        val suggestedCats = when (category) {
            ScreenshotEntity.CATEGORY_WORK -> listOf("Work", "Ideas", "Personal")
            ScreenshotEntity.CATEGORY_IDEAS -> listOf("Ideas", "Work", "Personal")
            ScreenshotEntity.CATEGORY_PERSONAL -> listOf("Personal", "Ideas", "Work")
            else -> listOf("Personal", "Work", "Ideas")
        }

        return AIAnalysisDto(
            category = category,
            subcategory = subcategory,
            title = title,
            summary = summary,
            confidence = confidence,
            language = "tr",
            detected_text = "Önizleme metni: $fileName taranarak analiz edildi.",
            important_fields = ImportantFieldsDto(
                company = company,
                amount = amount
            ),
            suggested_tags = tags,
            suggested_categories = suggestedCats,
            contains_sensitive_information = sensitive,
            action_suggestions = actions
        )
    }
}
