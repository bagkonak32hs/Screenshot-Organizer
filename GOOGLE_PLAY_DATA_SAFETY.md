# Google Play Veri Güvenliği Formu Yanıtları (Data Safety Questionnaire)

Google Play Console'a uygulama yüklerken doldurulması zorunlu olan **Veri Güvenliği (Data Safety)** anketini doldururken aşağıdaki yanıtları birebir seçebilirsiniz:

---

### 1. Veri Toplama ve Güvenlik Genel Soruları

| Soru | Yanıt | Açıklama |
| :--- | :--- | :--- |
| **Uygulamanız belirtilen kullanıcı verisi türlerinden herhangi birini topluyor veya paylaşıyor mu?** | **EVET (Yes)** | Kullanıcı analiz başlattığında ekran görüntüsü (Fotoğraflar) Google Gemini API'ye gönderilir (aşağıdaki tabloya bakın). Başka veri cihazdan çıkmaz. |
| **Uygulamanızın topladığı tüm kullanıcı verileri aktarım sırasında şifreleniyor mu?** | **EVET (Yes)** | Yalnızca kullanıcının açık isteğiyle yapılan Gemini API analiz çağrıları HTTPS/TLS ile uçtan uca şifrelenir. |
| **Kullanıcıların bu verilerin silinmesini talep edebileceği bir yöntem sağlıyor musunuz?** | **EVET (Yes)** | Kullanıcı dilediği an uygulama içinden görseli, önbelleği veya tüm uygulama verilerini anında silebilir. |
| **Uygulamanız çocuklara yönelik mi (Designed for Families)?** | **HAYIR (No)** | Hedef kitle: 13 yaş ve üzeri. |

---

#### Beyan edilecek veri türleri

| Veri türü | Toplanıyor mu? | Paylaşılıyor mu? | Amaç | Not |
| :--- | :--- | :--- | :--- | :--- |
| **Fotoğraflar** (ekran görüntüleri) | Evet | Evet (Google - Gemini API, hizmet sağlayıcı) | Uygulama işlevselliği | Yalnızca kullanıcı analiz başlatınca gönderilir; isteğe bağlıdır; aktarımda şifrelenir; uygulama sunucuda saklamaz. |
| **Ad ve e-posta** | Hayır | Hayır | - | Yerel hesap bilgileri yalnızca cihazda (şifre tuzlu hash olarak) tutulur, cihazdan çıkmaz. |

> Gönderilen görsellerde IBAN, kart, şifre gibi hassas bilgi bulunabilir; uygulama içinde analiz öncesi kullanıcıya bilgi verilmelidir.

---

### 2. İzin Beyanları ve Gerekçeleri (Permissions Declarations)

Google Play inceleme ekibi, depolama/medya izinleri için gerekçe metni isterse:

#### A. Fotoğraflar ve Videolar (`READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`)
* **Kullanım Amacı:** Temel İşlevsellik (Core App Functionality - Screenshot Management).
* **İnceleme Ekibine Açıklama (Reviewer Note):**
  > "Screenshot Organizer is a dedicated screenshot utility and cleanup manager. The app requires READ_MEDIA_IMAGES permission solely to detect user's screenshots in the local storage, present previews, identify burst/duplicate captures to save storage space, and allow the user to categorize or delete them. Images remain private on the user's device and are never harvested or sold."

#### B. Bildirimler (`POST_NOTIFICATIONS`)
* **Kullanım Amacı:** Kullanıcı tarafından talep edilen hatırlatıcılar ve yeni ekran görüntüsü algılandığında yerel bilgilendirme.
* **İnceleme Ekibine Açıklama:**
  > "Used to post local notifications for user-scheduled reminders and newly captured screenshot processing alerts."

---

### 3. Hedef Kitle ve İçerik Derecelendirmesi (IARC Content Rating)
* **Şiddet / Korku:** Yok
* **Cinsellik / Çıplaklık:** Yok
* **Küfür / Argo:** Yok
* **Uyuşturucu / Alkol / Kumar:** Yok
* **Kullanıcılar Arası Etkileşim / Sohbet:** Yok
* **Konum Paylaşımı:** Yok
* **Dijital Ürün Satışı:** Yok
* **Sonuç:** **PEGI 3 / Everyone (Tüm Yaş Grupları)** derecelendirmesi alır.

---

### 4. Reklam Kimliği (Advertising ID)
* Soru: "Uygulamanız reklam kimliği (AAID) kullanıyor mu?"
* Yanıt: **HAYIR (No)** — Uygulamada reklam SDK'sı bulunmamaktadır.
