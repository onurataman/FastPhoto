# FastPhoto - Setup Instructions

## Android Studio'da Açmak İçin

### 1. Gradle Sync
Projeyi Android Studio'da açtıktan sonra:
```
File → Sync Project with Gradle Files
```
veya üst toolbar'daki "Sync" butonuna basın.

### 2. Clean Build
Eğer hata alırsanız:
```
Build → Clean Project
Build → Rebuild Project
```

### 3. İlk Çalıştırma
1. Bir Android cihaz bağlayın veya AVD (emulator) oluşturun
2. Run → Run 'app' (veya Shift+F10)

## Yaygın Hatalar ve Çözümleri

### "Error loading build artifacts" Hatası
**Çözüm:**
1. Android Studio'yu kapatın
2. Projeyi yeniden açın
3. Gradle sync bekleyin
4. Clean & Rebuild yapın

### "SDK not found" Hatası
**Çözüm:**
1. File → Project Structure → SDK Location
2. Android SDK path'i doğru mu kontrol edin
3. SDK 34 kurulu mu kontrol edin

### Gradle Download Sorunları
**Çözüm:**
1. İnternet bağlantınızı kontrol edin
2. Gradle daemon'ı yeniden başlatın:
   ```
   ./gradlew --stop
   ./gradlew clean
   ```

## Gereksinimler
- Android Studio Hedgehog (2023.1.1) veya üstü
- JDK 17
- Android SDK 34
- Min SDK 29

## İlk Build Süresi
İlk build 5-10 dakika sürebilir (gradle dependencies download).

## Test Etme
Gerçek bir cihaz kullanmanız önerilir (özellikle gesture testleri için).
Samsung S24 Ultra veya benzeri büyük ekranlı cihazlarda en iyi performansı gösterir.
