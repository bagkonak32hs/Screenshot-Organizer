# Gizlilik Politikası (Privacy Policy)
**Son Güncelleme:** 17 Ekim 2026  
**Uygulama Adı:** Screenshot Organizer (com.selcu.screenshotorganizer)  
**İletişim:** bagkonak32hs@gmail.com  

Screenshot Organizer ("biz", "uygulamamız"), kullanıcılarımızın gizliliğine ve kişisel verilerinin güvenliğine en üst düzeyde önem verir. Bu Gizlilik Politikası, Screenshot Organizer Android uygulamasını kullandığınızda hangi verilerin işlendiğini, nasıl korunduğunu ve haklarınızı açıklamaktadır.

---

### 1. Toplanan ve İşlenen Veriler
Uygulamamız temel olarak **cihaz içi (on-device) gizlilik odaklı** mimariyle tasarlanmıştır.

1. **Fotoğraflar ve Medya Dosyaları (Ekran Görüntüleri):**
   * **İzin:** `android.permission.READ_MEDIA_IMAGES`, `android.permission.READ_MEDIA_VISUAL_USER_SELECTED` veya `android.permission.READ_EXTERNAL_STORAGE` (Android 12 ve öncesi).
   * **Amaç:** Cihazınızın ekran görüntüsü klasöründeki görselleri tespit etmek, küçük resimlerini (thumbnail) göstermek, yinelenen / seri çekimleri temizlemek ve kullanıcı talebi doğrultusunda kategorize etmek.
   * **Saklama:** Ekran görüntüleriniz sunucularımıza yüklenmez; tüm fotoğraflar cihazınızın yerel depolama alanında kalır.

2. **Uygulama İçi Meta Veriler ve Yerel Veritabanı:**
   * Ekran görüntülerine ait etiketler, kullanıcı notları, favori durumu ve kategori bilgileri cihazınızdaki korumalı yerel SQLite (Room) veritabanında saklanır.
   * Bu veriler üçüncü şahıslarla veya reklam ağlarıyla paylaşılmaz.

3. **Yapay Zekâ (Gemini API) Analizi:**
   * Yalnızca kullanıcının açık isteğiyle ("Analiz Et" veya "Akıllı Düzenle" butonları) seçilen ekran görüntüleri, içerik türünün belirlenmesi (örn. fatura, not, iş belgesi) amacıyla şifrelenmiş (TLS/HTTPS) güvenli kanallar üzerinden Google Gemini API servisine iletilir.
   * Analiz tamamlandığında elde edilen özet ve etiket bilgisi yerel veritabanınıza kaydedilir. Görselleriniz kalıcı bir üçüncü taraf sunucu veritabanında depolanmaz veya saklanmaz.

4. **Yerel Hesap Bilgileri:**
   * Uygulama içinde oluşturduğunuz hesabın adı ve e-posta adresi yalnızca cihazınızda saklanır; şifreniz tuzlu hash olarak tutulur ve hiçbir sunucuya gönderilmez. Bu bilgiler bulut yedeğine dahil edilmez.
   * Hesaplar cihaza özel olduğundan e-posta ile şifre sıfırlama yapılamaz.

5. **Bildirim İzni:**
   * `android.permission.POST_NOTIFICATIONS` izni yalnızca yeni ekran görüntüleri tespit edildiğinde veya kullanıcı tarafından planlanmış hatırlatıcılar zamanlandığında yerel bildirim göstermek için kullanılır.

---

### 2. Veri Paylaşımı ve Üçüncü Taraflar
* **Kişisel Verilerin Satışı:** Kişisel verileriniz veya fotoğraflarınız hiçbir koşulda satılmaz, kiralanmaz veya ticari amaçla üçüncü taraflara devredilmez.
* **Google (Gemini API):** Analiz için gönderilen görseller Google'ın Gemini API hizmet şartları ve gizlilik politikasına tabidir. Görsellerde kişisel veya hassas bilgi (IBAN, kimlik vb.) bulunabileceğinden, analizi yalnızca göndermek istediğiniz görseller için başlatın.
* **Reklam Ağları:** Uygulama içerisinde kullanıcı hareketlerini izleyen (tracking) veya profil oluşturan herhangi bir üçüncü taraf reklam SDK'sı (AdMob, Facebook Audience Network vb.) bulunmamaktadır.

---

### 3. Veri Güvenliği
* Uygulama, Google Play ve Android güvenlik standartlarına tam uyumludur.
* Tüm harici ağ iletişimleri (API çağrıları) modern TLS/HTTPS şifrelemesi ile korunmaktadır.
* Cihazınızdaki veritabanı Android uygulama kum havuzu (sandbox) izolasyonunda tutulur; başka uygulamalar tarafından erişilemez.

---

### 4. Veri Silme ve Kullanıcı Hakları (Data Deletion)
* **Tek Tuşla Silme:** Uygulama içerisinden dilediğiniz ekran görüntüsünü, yinelenen kopyaları veya uygulamanın tüm yerel veritabanı önbelleğini istediğiniz an silebilirsiniz.
* **Uygulamayı Kaldırma:** Uygulamayı cihazınızdan kaldırdığınızda, uygulamanın oluşturduğu tüm yerel veritabanı ve etiketler Android işletim sistemi tarafından kalıcı olarak silinir.
* Destek veya veri silme talepleri için bizimle **bagkonak32hs@gmail.com** adresi üzerinden dilediğiniz zaman iletişime geçebilirsiniz.

---

### 5. Çocukların Gizliliği
Screenshot Organizer 13 yaş altındaki çocuklardan bilerek herhangi bir kişisel veri toplamaz. Uygulamamız genel kitleye hitap eden bir üretkenlik ve galeri düzenleme aracıdır.

---

### 6. Politika Güncellemeleri
Bu Gizlilik Politikası zaman zaman güncellenebilir. Herhangi bir değişiklik bu sayfada yayınlanarak yürürlüğe girer.

**İletişim:**  
E-posta: bagkonak32hs@gmail.com  
Geliştirici: Screenshot Organizer Ekibi
