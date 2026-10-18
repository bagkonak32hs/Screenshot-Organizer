# Google Play Store Yayınlama Kılavuzu (2026 Güncel)

Bu kılavuz, **Screenshot Organizer** uygulamanızı Google Play Store'da resmi olarak yayınlamak için gereken tüm adımları, güncel Google SDK gereksinimlerini ve Google Play Console politikalarını içermektedir.

---

## 📋 1. Güncel Google SDK & Sistem Uyumluluk Durumu

Projeniz Google Play'in 2026 yılındaki tüm teknik şartlarına göre yapılandırılmıştır:

| Parametre | Değer | Google Play Şartı | Durum |
| :--- | :--- | :--- | :--- |
| **Minimum SDK (`minSdk`)** | **24** (Android 7.0) | En az 21+ | ✅ Mükemmel (Cihazların %98'ini kapsar) |
| **Hedef SDK (`targetSdk`)** | **36** (Android 15+) | En az 34/35 | ✅ En güncel Google şartlarını karşılar |
| **Derleme SDK (`compileSdk`)** | **36** | En az 34/35 | ✅ En güncel API desteği |
| **Paket Formatı** | **AAB (.aab)** | Yeni uygulamalarda AAB Zorunlu | ✅ App Bundle desteği hazır |
| **64-bit Desteği** | arm64-v8a / x86_64 | 64-bit Zorunlu | ✅ Otomatik dahil |
| **Uyarlanabilir Simge (Adaptive Icon)** | Vector + AnyDPI | Zorunlu | ✅ Material You uyumlu simge hazır |
| **R8 / ProGuard** | `proguard-rules.pro` | Güvenlik & Boyut optimizasyonu | ✅ Üretime hazır kurallar eklendi |

---

## 🛠️ 2. APK ve AAB (App Bundle) Dosyalarını Üretme

Google Play Store'a yükleme yaparken **.aab (Android App Bundle)** dosyası yüklenir. Google, kullanıcıların telefonuna en uygun boyuttaki APK'yı otomatik olarak Play Store üzerinden dağıtır.

### Seçenek A: AI Studio Arayüzünden Tek Tuşla İndirme (En Kolay)
1. AI Studio sağ üst köşesindeki **Ayarlar (Settings / Export)** menüsüne tıklayın.
2. **"Export APK"** veya **"Export AAB"** seçeneğine basarak doğrudan imzalı dosyanızı bilgisayarınıza indirin.

### Seçenek B: Terminal (Gradle) Komutuyla Oluşturma
* **Google Play için App Bundle (.aab) üretmek için:**
  ```bash
  gradle :app:bundleRelease
  ```
  *(Çıktı konumu: `app/build/outputs/bundle/release/app-release.aab`)*

* **Doğrudan Telefonda Test Etmek İçin APK üretmek için:**
  ```bash
  gradle :app:assembleDebug
  ```
  *(Çıktı konumu: `app/build/outputs/apk/debug/app-debug.apk`)*

---

## 🚀 3. Adım Adım Google Play Console Süreci

### Adım 1: Google Play Console Hesabı
1. [play.google.com/console](https://play.google.com/console) adresine gidin.
2. Google geliştirici hesabı açın (tek seferlik 25$ kayıt ücreti).

### Adım 2: Yeni Uygulama Oluşturma
1. **"Uygulama Oluştur" (Create App)** butonuna tıklayın.
2. Bilgileri girin:
   * **Uygulama Adı:** `Screenshot Organizer`
   * **Varsayılan Dil:** Türkçe (veya İngilizce)
   * **Tür:** Uygulama (App)
   * **Ücret Modeli:** Ücretsiz (Free)
   * Beyanları kabul edin ve devam edin.

### Adım 3: Uygulama İçeriği ve Politikalar (Zorunlu Alanlar)
Google Play sol menüsündeki **"Uygulama İçeriği" (App Content)** sekmesindeki tüm maddeleri doldurun:
1. **Gizlilik Politikası (Privacy Policy):**
   * Projenizdeki `PRIVACY_POLICY.md` dosyasını web'de (örneğin GitHub Pages, Google Sites veya Notion üzerinden) herkese açık bir bağlantı haline getirip URL'yi buraya yapıştırın.
2. **Uygulama Erişimi (App Access):**
   * *"Tüm işlevler özel erişim kısıtlamaları olmadan kullanılabilir"* seçeneğini işaretleyin (Giriş şifresi gerekmez).
3. **Reklamlar (Ads):**
   * *"Hayır, uygulamam reklam içermiyor"* seçin.
4. **İçerik Derecelendirmesi (Content Rating):**
   * Anketi başlatın. E-posta adresinizi girin. Kategori olarak "Üretkenlik / Yardımcı Programlar" seçin.
   * Tüm şiddet, cinsel içerik vb. sorulara "Hayır" deyin.
   * Sonuçta **PEGI 3 (Tüm Yaşlar)** onayını alın.
5. **Hedef Kitle ve İçerik (Target Audience):**
   * Yaş grubu olarak **13-15, 16-17, 18 ve üzeri** seçin.
   * *"Uygulamanız kasıtsız olarak çocukların ilgisini çekebilir mi?"* -> "Hayır" seçin.
6. **Veri Güvenliği (Data Safety):**
   * Projedeki `GOOGLE_PLAY_DATA_SAFETY.md` dosyasındaki yanıtları birebir işaretleyin.
7. **Hükümet / Finans Uygulamaları:**
   * "Hayır" olarak işaretleyin.

---

## 🎨 4. Mağaza Girişi Görselleri (Store Assets)

Sol menüden **"Ana Mağaza Girişi" (Main Store Listing)** bölümüne gidin:

1. **Metinler:**
   * `GOOGLE_PLAY_STORE_LISTING.md` dosyasındaki **Kısa Açıklama** ve **Tam Açıklama** metinlerini yapıştırın.
2. **Uygulama Simgesi:**
   * Boyut: **512 x 512 px**, PNG (32-bit), En fazla 1 MB.
3. **Öne Çıkan Grafik (Feature Graphic):**
   * Boyut: **1024 x 500 px**, PNG veya JPEG (Arka plan görseli).
4. **Telefon Ekran Görüntüleri:**
   * En az 2 adet (Önerilen: 4-6 adet) dikey ekran görüntüsü (1080x1920 veya 1080x2400 px).
   * Uygulamanın Ana Sayfasını, Akıllı Temizlik ekranını ve Detay ekranını yükleyin.

---

## 🚢 5. Sürümü Yükleme ve Yayınlama

1. Sol menüden **"Üretim" (Production)** veya **"Kapalı Test" (Closed Testing)** sekmesine gidin.
   *(Not: 13 Kasım 2023 sonrasında açılan yeni kişisel geliştirici hesaplarında Google, üretime geçmeden önce 20 test kullanıcısıyla 14 günlük Kapalı Test şartı koşar).*
2. **"Yeni Sürüm Oluştur" (Create New Release)** butonuna tıklayın.
3. **Google Play App Signing (Play İmzası)** seçeneğini etkinleştirin (Önerilen).
4. Ürettiğiniz `.aab` dosyasını sürüm kutusuna sürükleyip bırakın.
5. Sürüm adı: `1.0.0`
6. Sürüm notu olarak şunu yazabilirsiniz:
   ```text
   Screenshot Organizer ilk sürümü yayında!
   - Yapay zekâ ile akıllı ekran görüntüsü analizi
   - Çift ve seri çekim ekran görüntülerini tek tuşla temizleme
   - Güçlü OCR ve içerik araması
   ```
7. **"Kaydet"** ve ardından **"Sürümü İncele"** butonuna basarak Google inceleme ekibine gönderin.

Uygulamanız Google inceleme ekibi tarafından genellikle 24-48 saat içinde onaylanarak tüm dünyada yayına alınacaktır!
