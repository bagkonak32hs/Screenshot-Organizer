package com.example.data.local

import com.example.data.model.CategoryEntity
import com.example.data.model.ScreenshotEntity

object DemoDataProvider {

    fun getDefaultCategories(): List<CategoryEntity> {
        return listOf(
            // Core Primary Categories (İş / Work, Kişisel / Personal, Fikirler / Ideas, Okul / School, Eğlence / Entertainment)
            CategoryEntity(
                id = ScreenshotEntity.CATEGORY_WORK,
                name = "İş / Work",
                iconName = "Work",
                colorHex = "#2563EB",
                description = "İş yazışmaları, sunumlar, raporlar, kod kesitleri, Slack/Teams ve kurumsal projeler"
            ),
            CategoryEntity(
                id = ScreenshotEntity.CATEGORY_PERSONAL,
                name = "Kişisel / Personal",
                iconName = "Person",
                colorHex = "#8B5CF6",
                description = "Özel sohbetler, aile, sağlık randevuları, kişisel notlar ve günlük anılar"
            ),
            CategoryEntity(
                id = ScreenshotEntity.CATEGORY_IDEAS,
                name = "Fikirler & İlham / Ideas",
                iconName = "Lightbulb",
                colorHex = "#EAB308",
                description = "Yaratıcı fikirler, tasarım ilhamları, beyin fırtınası notları, alıntılar ve proje taslakları"
            ),
            CategoryEntity(
                id = ScreenshotEntity.CATEGORY_SCHOOL,
                name = "Okul / School",
                iconName = "School",
                colorHex = "#059669",
                description = "Ders notları, ödevler, sınav tarihleri, üniversite/okul duyuruları ve akademik slaytlar"
            ),
            CategoryEntity(
                id = ScreenshotEntity.CATEGORY_ENTERTAINMENT,
                name = "Eğlence / Entertainment",
                iconName = "SportsEsports",
                colorHex = "#F59E0B",
                description = "Oyunlar, filmler, diziler, YouTube, sosyal medya kesitleri, müzik ve popüler kültür"
            ),

            // Supporting specialized categories
            CategoryEntity(
                id = "finance",
                name = "Banka & Finans",
                iconName = "AccountBalance",
                colorHex = "#0D9488",
                description = "EFT/Havale dekontları, IBAN ekranları ve portföy durumları"
            ),
            CategoryEntity(
                id = "invoice",
                name = "Faturalar",
                iconName = "ReceiptLong",
                colorHex = "#EF4444",
                description = "Elektrik, su, doğalgaz, internet ve telefon faturaları"
            ),
            CategoryEntity(
                id = "receipt",
                name = "Makbuz & Fişler",
                iconName = "Receipt",
                colorHex = "#EC4899",
                description = "Market, kafe, restoran fişleri ve perakende makbuzları"
            ),
            CategoryEntity(
                id = "chat",
                name = "Konuşmalar",
                iconName = "Chat",
                colorHex = "#3B82F6",
                description = "WhatsApp, Telegram ve mesajlaşma kesitleri"
            ),
            CategoryEntity(
                id = "note",
                name = "Notlar",
                iconName = "StickyNote2",
                colorHex = "#EAB308",
                description = "El yazısı notlar, yapılacaklar listeleri ve karalamalar"
            ),
            CategoryEntity(
                id = "address",
                name = "Adresler",
                iconName = "LocationOn",
                colorHex = "#10B981",
                description = "Konumlar, teslimat ve buluşma adresleri"
            ),
            CategoryEntity(
                id = "product",
                name = "Alışveriş & Ürünler",
                iconName = "ShoppingBag",
                colorHex = "#FB923C",
                description = "Alışveriş sepetleri, ürün görselleri ve fiyat karşılaştırmaları"
            ),
            CategoryEntity(
                id = "travel",
                name = "Seyahat",
                iconName = "Flight",
                colorHex = "#06B6D4",
                description = "Uçak biletleri, tren kartları ve seyahat planları"
            ),
            CategoryEntity(
                id = "other",
                name = "Diğer",
                iconName = "Folder",
                colorHex = "#94A3B8",
                description = "Henüz sınıflandırılmamış veya karma ekran görüntüleri"
            )
        )
    }

    fun getInitialDemoScreenshots(): List<ScreenshotEntity> {
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        return listOf(
            // 1. İŞ (Work) - Project Roadmap & Sprint
            ScreenshotEntity(
                id = 1,
                uri = "demo://screenshot/work_project_q4",
                fileName = "Screenshot_20261014_114510_Slack_Sprint_Q4.png",
                createdAt = now - (dayMs * 1),
                categoryId = ScreenshotEntity.CATEGORY_WORK,
                subcategory = "project_roadmap",
                title = "Sprint Q4 Yol Haritası & Görev Dağılımı",
                summary = "Slack proje kanalından kaydedilen sprint hedefleri, Gemini Vision API teslim tarihleri ve iş dağılımı tablosu.",
                ocrText = """
                    Slack - #product-engineering
                    Ekim 2026 Sprint Planlama Notları
                    - Mobil UI revizyonu: 12 Ekim (Bitti)
                    - Gemini Vision API entegrasyonu: 15 Ekim
                    - WorkManager arka plan tarama: 18 Ekim
                    - QA ve Güvenlik Testleri: 22 Ekim
                    Sorumlular: Selin (UI/UX), Emre (Backend), Can (Mobile)
                    Toplantı Linki: meet.google.com/xyz-work-plan
                """.trimIndent(),
                confidence = 0.98f,
                isFavorite = true,
                isSensitive = false,
                isHidden = false,
                isAnalyzed = true,
                analysisStatus = "COMPLETED",
                company = "Product Engineering Team",
                tagsJson = "İş, Work, Sprint, Proje, Planlama, Slack, Mühendislik",
                actionSuggestion = "Sprint teslim tarihlerini Google Takvim'e aktar",
                isDemo = true
            ),

            // 2. OKUL (School) - Midterm Exam & Course Notes
            ScreenshotEntity(
                id = 2,
                uri = "demo://screenshot/school_algorithms_midterm",
                fileName = "Screenshot_20261013_142010_CENG_Algoritmalar_Vize.png",
                createdAt = now - (dayMs * 2),
                categoryId = ScreenshotEntity.CATEGORY_SCHOOL,
                subcategory = "midterm_exam",
                title = "İleri Algoritmalar Vize Sınavı & Ödev Konuları",
                summary = "Bilgisayar Mühendisliği dersi dinamik programlama, graf teorisi vize kapsamı ve ödev teslim tarihi.",
                ocrText = """
                    İTÜ BİLGİSAYAR MÜHENDİSLİĞİ - BLG 335E İLERİ ALGORİTMALAR
                    Ders Sorumlusu: Prof. Dr. Kemal Arslan
                    VİZE SINAV TARİHİ: 28 Ekim 2026 Çarşamba 14:00 (D-201 Amfisi)
                    Vize Konu Kapsamı:
                    1. Asymptotic Notation & Recurrence Relations
                    2. Divide and Conquer, Dynamic Programming (0/1 Knapsack, LCS)
                    3. Greedy Algorithms & Huffman Coding
                    4. Graph Traversal: BFS, DFS, Dijkstra, Bellman-Ford
                    Ödev 1 Teslimi: 20 Ekim 2026 23:59 (Ninova üzerinden PDF formatında)
                """.trimIndent(),
                confidence = 0.99f,
                isFavorite = true,
                isSensitive = false,
                isHidden = false,
                isAnalyzed = true,
                analysisStatus = "COMPLETED",
                company = "İTÜ Bilgisayar Mühendisliği",
                dateStr = "2026-10-28",
                tagsJson = "Okul, Sınav, Algoritmalar, Üniversite, Ödev, Vize, Dinamik Programlama",
                actionSuggestion = "Vize sınav tarihini (28 Ekim) takvime kaydet ve hatırlatıcı kur",
                isDemo = true
            ),

            // 3. EĞLENCE (Entertainment) - Steam Autumn Sale & Gaming
            ScreenshotEntity(
                id = 3,
                uri = "demo://screenshot/entertainment_steam_sale",
                fileName = "Screenshot_20261014_221530_Steam_Cadilar_Bayrami.png",
                createdAt = now - (dayMs * 1) + 3600000L,
                categoryId = ScreenshotEntity.CATEGORY_ENTERTAINMENT,
                subcategory = "pc_gaming",
                title = "Steam Cadılar Bayramı İndirimleri & İstek Listesi",
                summary = "Cyberpunk 2077 Phantom Liberty ve Hades II indirim fırsatları ve oyun süresi başarımları.",
                ocrText = """
                    STEAM STORE - SPECIAL OFFERS
                    Cadılar Bayramı & Sonbahar Festivali İndirimleri
                    1. Cyberpunk 2077: Phantom Liberty Bundle - %45 İndirim (34.99 USD)
                    2. Hades II - Erken Erişim Güncellemesi (21.99 USD)
                    3. Baldur's Gate 3 - Yılın Oyunu Sürümü (39.99 USD)
                    İstek Listenizdeki 4 oyun şu an indirimde!
                    Kampanya Bitiş Tarihi: 2 Kasım 2026 20:00 TSİ
                """.trimIndent(),
                confidence = 0.97f,
                isFavorite = true,
                isSensitive = false,
                isHidden = false,
                isAnalyzed = true,
                analysisStatus = "COMPLETED",
                company = "Steam / Valve",
                amount = "34.99 USD",
                dateStr = "2026-11-02",
                tagsJson = "Eğlence, Oyun, Steam, Cyberpunk, Hades, İndirim, Gaming",
                actionSuggestion = "İndirim bitiş tarihini takip et (2 Kasım 2026)",
                isDemo = true
            ),

            // 4. KİŞİSEL (Personal) - Family WhatsApp Chat & Weekend Plan
            ScreenshotEntity(
                id = 4,
                uri = "demo://screenshot/personal_family_chat",
                fileName = "Screenshot_20261013_184512_Aile_Grubu_WhatsApp.png",
                createdAt = now - (dayMs * 2) + 7200000L,
                categoryId = ScreenshotEntity.CATEGORY_PERSONAL,
                subcategory = "family_chat",
                title = "Aile Grubu - Pazar Kahvaltısı & Alışveriş",
                summary = "Annem ve aile grubuyla organize edilen hafta sonu kahvaltı buluşması ve alınacaklar listesi.",
                ocrText = """
                    WhatsApp - Bizim Aile (4 Katılımcı)
                    Annem: Canım bu pazar sabah 10:30'da hep beraber kahvaltıya bekliyorum.
                    Gelecekler: Selin, Burak, Babam ve ben.
                    Alınacaklar listesi:
                    - Fırından taze simit ve cevizli ekmek
                    - Ezine beyaz peynir ve tulum peyniri
                    - Çengelköy salatalığı, domates
                    Burak: Tamamdır annecim fırın bende, saatinde oradayız!
                """.trimIndent(),
                confidence = 0.96f,
                isFavorite = true,
                isSensitive = false,
                isHidden = false,
                isAnalyzed = true,
                analysisStatus = "COMPLETED",
                company = "Aile",
                dateStr = "2026-10-18",
                tagsJson = "Kişisel, Aile, WhatsApp, Kahvaltı, Hafta Sonu, Sohbet",
                actionSuggestion = "Pazar kahvaltısı için fırın alışverişi hatırlatıcısı oluştur",
                isDemo = true
            ),

            // 5. İŞ (Work) - GitHub Pull Request Code Review
            ScreenshotEntity(
                id = 5,
                uri = "demo://screenshot/work_github_pr",
                fileName = "Screenshot_20261010_163022_GitHub_PR.png",
                createdAt = now - (dayMs * 5),
                categoryId = ScreenshotEntity.CATEGORY_WORK,
                subcategory = "code_review",
                title = "GitHub PR #142 - Gemini Vision Classifier & Room",
                summary = "Android uygulaması için Room DAO mimarisi ve Kotlin Coroutines PR inceleme yorumları.",
                ocrText = """
                    GitHub - repository: ai-screenshot-organizer
                    Pull Request #142: feat(ai): integrate gemini-3.5-flash with room database
                    Author: can-dev (Can Yılmaz)
                    Status: Approved by 2 reviewers (selin-ux, emre-backend)
                    Changes: +480 lines, -24 lines
                    Key modules:
                    - GeminiVisionService.kt: multimodal prompt for 4 core categories
                    - ScreenshotDao.kt: query methods for work, school, entertainment, personal
                    - StoragePermissionHelper.kt: Android 14+ permission flow
                """.trimIndent(),
                confidence = 0.98f,
                isFavorite = false,
                isSensitive = false,
                isHidden = false,
                isAnalyzed = true,
                analysisStatus = "COMPLETED",
                company = "GitHub",
                invoiceNumber = "PR #142",
                tagsJson = "İş, Work, GitHub, PR, Kotlin, Android, Kod İnceleme",
                actionSuggestion = "PR onaylandıktan sonra main branch'e merge et",
                isDemo = true
            ),

            // 6. OKUL (School) - Weekly Course Timetable
            ScreenshotEntity(
                id = 6,
                uri = "demo://screenshot/school_course_schedule",
                fileName = "Screenshot_20261006_091022_Guz_Ders_Programi.png",
                createdAt = now - (dayMs * 8),
                categoryId = ScreenshotEntity.CATEGORY_SCHOOL,
                subcategory = "course_schedule",
                title = "2026-2027 Güz Yarıyılı Haftalık Ders Programı",
                summary = "Haftalık üniversite ders çizelgesi, laboratuvar saatleri ve amfi derslik bilgileri.",
                ocrText = """
                    ÖĞRENCİ İŞLERİ DAİRE BAŞKANLIĞI - HAFTALIK DERS ÇİZELGESİ
                    Öğrenci: Selin Demir - 150200841
                    Pazartesi:
                    - 09:30 - 12:30: BLG 335E İleri Algoritmalar (Amfi EEB-101)
                    Salı:
                    - 13:30 - 16:30: YAP 402E Yapay Zekâya Giriş (Derslik B-204)
                    Çarşamba:
                    - 10:30 - 12:30: Mobil Uygulama Geliştirme Laboratuvarı (Lab-3)
                    Perşembe:
                    - 14:30 - 17:30: Veri Tabanı Sistemleri (Amfi EEB-102)
                """.trimIndent(),
                confidence = 0.97f,
                isFavorite = false,
                isSensitive = false,
                isHidden = false,
                isAnalyzed = true,
                analysisStatus = "COMPLETED",
                company = "Üniversite Öğrenci İşleri",
                tagsJson = "Okul, Ders Programı, Üniversite, Amfi, Çizelge, Güz Dönemi",
                actionSuggestion = "Ders programını haftalık tekrarlayan takvim etkinliğine dönüştür",
                isDemo = true
            ),

            // 7. EĞLENCE (Entertainment) - Netflix Watchlist
            ScreenshotEntity(
                id = 7,
                uri = "demo://screenshot/entertainment_netflix_watchlist",
                fileName = "Screenshot_20261011_203015_Netflix_Series.png",
                createdAt = now - (dayMs * 4),
                categoryId = ScreenshotEntity.CATEGORY_ENTERTAINMENT,
                subcategory = "movies_series",
                title = "Netflix Hafta Sonu Dizi & Film İzleme Listesi",
                summary = "Black Mirror yeni sezon, bilim kurgu mini dizisi ve ödüllü film önerileri listesi.",
                ocrText = """
                    NETFLIX - TOP 10 IN TURKEY TODAY
                    İzleme Listeniz:
                    1. Black Mirror: Season 7 (Bilim Kurgu / Distopya - 98% Match)
                    2. Dune: Prophecy (Epik Bilim Kurgu - 6 Bölüm)
                    3. Hayao Miyazaki: Çocuk ve Balıkçıl (Anime / Macera)
                    4. Arcane: League of Legends Sezon 2
                    Süre: 4 saat 30 dk toplam izleme önerisi
                """.trimIndent(),
                confidence = 0.96f,
                isFavorite = false,
                isSensitive = false,
                isHidden = false,
                isAnalyzed = true,
                analysisStatus = "COMPLETED",
                company = "Netflix",
                tagsJson = "Eğlence, Netflix, Dizi, Film, Anime, Hafta Sonu, Sinema",
                actionSuggestion = "Hafta sonu film gecesi için favorilere ekle",
                isDemo = true
            ),

            // 8. KİŞİSEL (Personal) - Dentist Appointment Reminder
            ScreenshotEntity(
                id = 8,
                uri = "demo://screenshot/personal_dental_appointment",
                fileName = "Screenshot_20261008_113000_Dis_Randevusu.png",
                createdAt = now - (dayMs * 7),
                categoryId = ScreenshotEntity.CATEGORY_PERSONAL,
                subcategory = "health_appointment",
                title = "Özel Diş Kliniği Kontrol Randevusu",
                summary = "Dt. Merve Aksoy ile 24 Ekim Cumartesi 15:30 rutin diş kontrolü ve temizlik randevusu.",
                ocrText = """
                    DENTAPLUS AĞIZ VE DİŞ SAĞLIĞI POLİKLİNİĞİ
                    Randevu Onay Bildirimi
                    Hasta: Selin Demir
                    Hekim: Dt. Merve Aksoy
                    İşlem: Rutin 6 Aylık Kontrol ve Diş Taşı Temizliği
                    Tarih & Saat: 24 Ekim 2026 Cumartesi - 15:30
                    Klinik Adresi: Bağdat Cad. No: 182 Kat: 2 Kadıköy / İstanbul
                    İletişim: +90 216 385 40 40
                """.trimIndent(),
                confidence = 0.99f,
                isFavorite = true,
                isSensitive = false,
                isHidden = false,
                isAnalyzed = true,
                analysisStatus = "COMPLETED",
                company = "DentaPlus Polikliniği",
                dateStr = "2026-10-24",
                address = "Bağdat Cad. No: 182 Kadıköy / İstanbul",
                phoneNumber = "+90 216 385 40 40",
                tagsJson = "Kişisel, Sağlık, Randevu, Diş, Kadıköy, Doktor",
                actionSuggestion = "24 Ekim saat 15:30 için takvim randevusu oluştur",
                isDemo = true
            ),

            // 9. Supporting: Fatura (Invoice)
            ScreenshotEntity(
                id = 9,
                uri = "demo://screenshot/invoice_enerjisa",
                fileName = "Screenshot_20261001_143022_EnerjiSA.png",
                createdAt = now - (dayMs * 13),
                categoryId = "invoice",
                subcategory = "electricity_bill",
                title = "EnerjiSA Elektrik Faturası",
                summary = "2026 Eylül dönemi elektrik kullanım bedeli. Son ödeme tarihi 15 Ekim 2026.",
                ocrText = """
                    ENERJİSA ANADOLU YAKASI ELEKTRİK PERAKENDE SATIŞ A.Ş.
                    Fatura No: ENS202609012398
                    Tesisat No: 0014892019
                    Dönem: 2026/09
                    Ödenecek Tutar: 1.250,50 TL
                    Son Ödeme Tarihi: 15.10.2026
                """.trimIndent(),
                confidence = 0.98f,
                isFavorite = false,
                isSensitive = false,
                isHidden = false,
                isAnalyzed = true,
                analysisStatus = "COMPLETED",
                company = "EnerjiSA",
                amount = "1.250,50 TL",
                dateStr = "2026-10-15",
                invoiceNumber = "ENS202609012398",
                tagsJson = "Fatura, Elektrik, EnerjiSA, Ödeme, Kişisel",
                actionSuggestion = "Son ödeme tarihi için hatırlatıcı oluştur (15 Ekim 2026)",
                isDemo = true
            ),

            // 10. Supporting: Finans Dekontu (Finance)
            ScreenshotEntity(
                id = 10,
                uri = "demo://screenshot/bank_dekont",
                fileName = "Screenshot_20260929_181145_Garanti.png",
                createdAt = now - (dayMs * 15),
                categoryId = "finance",
                subcategory = "money_transfer",
                title = "Garanti BBVA Havale Dekontu",
                summary = "Ahmet Kaya adına kira ödemesi transfer dekontu.",
                ocrText = """
                    GARANTİ BBVA FAST / HAVALE BİLGİ FİŞİ
                    İşlem Tarihi: 29.09.2026 18:11:02
                    Gönderen: Selin Demir
                    Alıcı: Ahmet Kaya
                    IBAN: TR45 0006 2000 1234 5678 9012 34
                    Tutar: 24.500,00 TL
                    Açıklama: 2026 Ekim Ayı Kira Bedeli
                    Referans No: FAST-20260929-918231
                """.trimIndent(),
                confidence = 0.99f,
                isFavorite = false,
                isSensitive = true,
                isHidden = false,
                isAnalyzed = true,
                analysisStatus = "COMPLETED",
                company = "Garanti BBVA",
                amount = "24.500,00 TL",
                dateStr = "2026-09-29",
                invoiceNumber = "FAST-20260929-918231",
                tagsJson = "Dekont, Kira, Garanti, IBAN, Havale, Finans",
                actionSuggestion = "Kira makbuzunu 'Finans' koleksiyonunda güvenli sakla",
                isDemo = true
            ),

            // 11. FİKİRLER & İLHAM (Ideas) - Mobile UI/UX Design System Concept
            ScreenshotEntity(
                id = 11,
                uri = "demo://screenshot/ideas_design_concept",
                fileName = "Screenshot_20261014_112045_Mobile_Design_Inspiration.png",
                createdAt = now - (dayMs * 1),
                categoryId = ScreenshotEntity.CATEGORY_IDEAS,
                subcategory = "ui_inspiration",
                title = "Mobil Arayüz & Minimalist Kart Tasarımı Konsepti",
                summary = "Dribbble ve Behance üzerinden kaydedilen M3 uyumlu modern mobil dashboard kartı ve renk paleti ilhamı.",
                ocrText = """
                    DesignInspo - Mobile UX Trends 2026
                    Concept: Dynamic Adaptive Bento Grid Layout
                    Color Palette: Deep Indigo #2563EB, Warm Amber #EAB308, Soft Slate #64748B
                    Components:
                    - Floating Glanceable Metric Card
                    - Quick Gesture Swipes for AI Classification
                    - Seamless Optical Recognition Preview
                    Notes: Bu tasarım dilini bir sonraki açık kaynak projede uygulayabiliriz!
                """.trimIndent(),
                confidence = 0.98f,
                isFavorite = true,
                isSensitive = false,
                isHidden = false,
                isAnalyzed = true,
                analysisStatus = "COMPLETED",
                company = "DesignInspo",
                tagsJson = "Fikirler, İlham, Ideas, UI/UX, Tasarım, Bento Grid, Mobil, Konsept",
                suggestedCategoriesJson = "Ideas, Work, Personal",
                actionSuggestion = "İlham panosuna ekle ve Figma taslaklarına aktar",
                isDemo = true
            ),

            // 12. BURST DUPLICATE: Peş peşe çekilmiş Slack Sprint Planı (4 sn sonra mükerrer)
            ScreenshotEntity(
                id = 12,
                uri = "demo://screenshot/work_project_q4_burst",
                fileName = "Screenshot_20261014_114514_Slack_Sprint_Q4_Dup.png",
                createdAt = (now - (dayMs * 1)) + 4000L, // 4 seconds after id = 1
                categoryId = ScreenshotEntity.CATEGORY_WORK,
                subcategory = "project_roadmap",
                title = "Sprint Q4 Yol Haritası & Görev Dağılımı",
                summary = "Slack kanalından peş peşe çekilmiş mükerrer sprint planı ekran görüntüsü.",
                ocrText = """
                    Slack - #product-engineering
                    Ekim 2026 Sprint Planlama Notları
                    - Mobil UI revizyonu: 12 Ekim (Bitti)
                    - Gemini Vision API entegrasyonu: 15 Ekim
                    - WorkManager arka plan tarama: 18 Ekim
                    - QA ve Güvenlik Testleri: 22 Ekim
                    Sorumlular: Selin (UI/UX), Emre (Backend), Can (Mobile)
                    Toplantı Linki: meet.google.com/xyz-work-plan
                """.trimIndent(),
                confidence = 0.94f,
                isFavorite = false,
                isSensitive = false,
                isHidden = false,
                isAnalyzed = true,
                analysisStatus = "COMPLETED",
                company = "Product Engineering Team",
                tagsJson = "İş, Work, Sprint, Mükerrer",
                isDemo = true
            ),

            // 13. EXACT INVOICE DUPLICATE: EnerjiSA Elektrik Faturası Çift Çekimi (Aynı Fatura No)
            ScreenshotEntity(
                id = 13,
                uri = "demo://screenshot/invoice_enerjisa_dup",
                fileName = "Screenshot_20261001_143026_EnerjiSA_Kopya.png",
                createdAt = (now - (dayMs * 13)) + 4000L,
                categoryId = "invoice",
                subcategory = "electricity_bill",
                title = "EnerjiSA Elektrik Faturası (Tekrar)",
                summary = "Aynı fatura numarasına (ENS202609012398) sahip mükerrer fatura görüntüsü.",
                ocrText = """
                    ENERJİSA ANADOLU YAKASI ELEKTRİK PERAKENDE SATIŞ A.Ş.
                    Fatura No: ENS202609012398
                    Tesisat No: 0014892019
                    Dönem: 2026/09
                    Ödenecek Tutar: 1.250,50 TL
                    Son Ödeme Tarihi: 15.10.2026
                """.trimIndent(),
                confidence = 0.95f,
                isFavorite = false,
                isSensitive = false,
                isHidden = false,
                isAnalyzed = true,
                analysisStatus = "COMPLETED",
                company = "EnerjiSA",
                amount = "1.250,50 TL",
                dateStr = "2026-10-15",
                invoiceNumber = "ENS202609012398",
                tagsJson = "Fatura, Elektrik, Mükerrer",
                isDemo = true
            ),

            // 14. EXPIRED STALE OTP: Süresi Dolmuş 3 Dakikalık SMS Doğrulama Kodu
            ScreenshotEntity(
                id = 14,
                uri = "demo://screenshot/otp_verification_expired",
                fileName = "Screenshot_20261005_120110_SMS_Dogrulama.png",
                createdAt = now - (dayMs * 11),
                categoryId = "finance",
                subcategory = "sms_otp",
                title = "Banka Tek Kullanımlık SMS Onay Kodu",
                summary = "11 gün önce gelen ve 3 dakika geçerlilik süresi dolmuş tek kullanımlık güvenlik SMS kodu.",
                ocrText = """
                    BANKA SMS BİLGİLENDİRME
                    Değerli Müşterimiz, 24.500 TL transfer işleminiz için tek kullanımlık onay kodunuz: 492018.
                    Bu şifre 3 dakika geçerlidir. Güvenliğiniz için banka personeli dahil kimseyle paylaşmayınız.
                    Ref: OTP-991204
                """.trimIndent(),
                confidence = 0.99f,
                isFavorite = false,
                isSensitive = true,
                isHidden = false,
                isAnalyzed = true,
                analysisStatus = "COMPLETED",
                company = "Banka SMS",
                tagsJson = "SMS, Doğrulama Kodu, OTP, Süresi Dolmuş",
                isDemo = true
            )
        )
    }
}
