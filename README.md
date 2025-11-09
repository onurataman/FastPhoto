# 📱 FastPhoto - Akıllı Galeri Yöneticisi

FastPhoto, Android cihazlar için geliştirilmiş hafif, hızlı ve kullanıcı dostu bir galeri yönetim uygulamasıdır. Samsung Galaxy S24 Ultra gibi yüksek performanslı cihazlar için optimize edilmiştir.

## ✨ Özellikler

### 📁 Albüm Yönetimi
- Cihazdaki tüm fotoğraf albümlerini (Camera, WhatsApp, Screenshots vb.) listeler
- Her albüm için thumbnail ve fotoğraf sayısı gösterir
- Modern grid layout ile kolay gezinme

### 🖼️ Fotoğraf Görüntüleme
- Yatay kaydırma (swipe) ile fotoğraflar arasında gezinme
- Tam ekran görüntüleme
- Akıcı animasyonlar (120Hz ekran desteği)

### 🗑️ Akıllı Silme Sistemi
- Fotoğraflar silindiğinde anında çöp kutusuna taşınır
- Fiziksel dosya korunur, sadece galeri uygulamalarından gizlenir
- Yanlışlıkla silmelere karşı güvenlik

### 🔄 Geri Yükleme
- Çöp kutusundan tek dokunuşla geri yükleme
- Orijinal konuma ve MediaStore'a geri ekleme
- Basit ve hızlı işlem

### 🧹 Kalıcı Silme
- Çöp kutusundaki fotoğrafları kalıcı olarak silme
- Toplu silme özelliği (çöp kutusunu boşalt)
- Onay dialogları ile güvenli silme

### 🎨 Modern UI/UX
- Material Design 3 tasarım dili
- Dark/Light tema desteği
- Responsive layout - tüm ekran boyutlarına uyumlu
- Samsung S24 Ultra için optimize edilmiş

### 🔒 Gizlilik & Güvenlik
- Android Scoped Storage tam uyumlu
- Google Play politikalarına uygun
- Hiçbir veri dış sunucuya gönderilmez
- Sadece gerekli izinler istenir

## 🛠️ Teknik Özellikler

### Teknoloji Stack
- **Dil:** Kotlin
- **UI Framework:** Jetpack Compose
- **Mimari:** MVVM + Clean Architecture
- **Database:** Room (SQLite)
- **Image Loading:** Coil
- **Dependency Injection:** Hilt
- **Min SDK:** Android 10 (API 29)
- **Target SDK:** Android 14 (API 34)

### Proje Yapısı

```
FastPhoto/
├── app/
│   ├── src/main/
│   │   ├── java/com/fastphoto/app/
│   │   │   ├── data/
│   │   │   │   ├── local/          # Room Database
│   │   │   │   ├── model/          # Data models
│   │   │   │   └── repository/     # Repositories
│   │   │   ├── di/                 # Hilt modules
│   │   │   ├── ui/
│   │   │   │   ├── navigation/     # Navigation
│   │   │   │   ├── screens/        # UI Screens
│   │   │   │   ├── theme/          # Material Theme
│   │   │   │   └── viewmodel/      # ViewModels
│   │   │   ├── FastPhotoApplication.kt
│   │   │   └── MainActivity.kt
│   │   └── res/
│   └── build.gradle.kts
└── build.gradle.kts
```

## 🚀 Kurulum

### Gereksinimler
- Android Studio Hedgehog (2023.1.1) veya üstü
- JDK 17
- Android SDK 34
- Gradle 8.2.0+

### Adımlar

1. **Projeyi klonlayın:**
```bash
git clone https://github.com/yourusername/FastPhoto.git
cd FastPhoto
```

2. **Android Studio'da açın:**
- Android Studio'yu açın
- "Open an Existing Project" seçin
- FastPhoto klasörünü seçin

3. **Gradle sync:**
- Proje açıldığında otomatik olarak Gradle sync başlayacaktır
- Eğer başlamazsa: File → Sync Project with Gradle Files

4. **Build ve çalıştırın:**
- Bir Android cihaz bağlayın veya emulator başlatın
- Run → Run 'app' (veya Shift+F10)

## 📱 Kullanım

### İlk Çalıştırma
1. Uygulama açıldığında depolama izni isteyecektir
2. "İzin Ver" butonuna basın
3. Sistem dialog'unda izni onaylayın

### Albüm Görüntüleme
- Ana ekranda tüm albümler grid şeklinde listelenir
- Bir albüme dokunarak içindeki fotoğrafları görüntüleyin

### Fotoğraf Silme
1. Fotoğraf görüntüleyicide üst menüden çöp kutusu ikonuna basın
2. Onay dialogunda "Sil" butonuna basın
3. Fotoğraf çöp kutusuna taşınacaktır

### Geri Yükleme
1. Alt menüden "Çöp Kutusu" sekmesine geçin
2. Geri yüklemek istediğiniz fotoğrafa dokunun
3. Menüden "Geri Yükle" seçeneğini seçin

### Kalıcı Silme
1. Çöp kutusunda bir fotoğrafa dokunun
2. Menüden "Kalıcı Sil" seçeneğini seçin
3. Onay dialogunda "Sil" butonuna basın

## 🔧 Geliştirme

### Yeni Özellik Ekleme
1. Feature branch oluşturun: `git checkout -b feature/yeni-ozellik`
2. Değişikliklerinizi yapın
3. Test edin
4. Commit yapın: `git commit -m "feat: yeni özellik açıklaması"`
5. Push edin: `git push origin feature/yeni-ozellik`
6. Pull request oluşturun

### Test Çalıştırma
```bash
# Unit testler
./gradlew test

# Android testler
./gradlew connectedAndroidTest
```

### Build Varyantları
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## 📋 Önemli Notlar

### Scoped Storage Uyumluluğu
- Android 10+ Scoped Storage kurallarına tam uyumludur
- `MANAGE_EXTERNAL_STORAGE` izni kullanılmaz (Google Play uyumlu)
- Çöp kutusu dosyaları app-specific directory'de saklanır: `/Android/data/com.fastphoto.app/files/trash/`

### İzinler
- **READ_MEDIA_IMAGES** (Android 13+): Fotoğrafları okumak için
- **READ_EXTERNAL_STORAGE** (Android 10-12): Fotoğrafları okumak için
- **ACCESS_MEDIA_LOCATION** (Opsiyonel): Fotoğraf metadata'sı için

### Performans Optimizasyonu
- Coil kütüphanesi ile lazy image loading
- Compose LazyGrid ile verimli liste görüntüleme
- Room Database ile hızlı metadata erişimi
- Coroutines ile asenkron işlemler

## 🐛 Bilinen Sorunlar

- Android 11+ cihazlarda silme işlemi bazen sistem onayı gerektirebilir
- Bazı cihazlarda çöp kutusundan geri yükleme yavaş olabilir (büyük dosyalarda)

## 🤝 Katkıda Bulunma

Katkılarınızı bekliyoruz! Lütfen pull request göndermeden önce:

1. Issue oluşturun
2. Feature branch kullanın
3. Kodunuzu test edin
4. Commit mesajlarını anlamlı yazın

## 📄 Lisans

Bu proje MIT lisansı altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.

## 👨‍💻 Geliştirici

- **Proje Adı:** FastPhoto
- **Paket Adı:** com.fastphoto.app
- **Min SDK:** 29 (Android 10)
- **Target SDK:** 34 (Android 14)

## 📞 İletişim

Sorularınız için issue açabilir veya email gönderebilirsiniz.

---

**FastPhoto** - Fotoğraflarınız güvende, kontrolünüzde! 📸
