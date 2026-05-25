# PhotoNext - Uygulama Geliştirme Planı

## 📋 Genel Bakış

Bu doküman, PhotoNext uygulamasının geliştirilmesi için adım adım bir plan sunar. Her aşama, spesifik görevler ve beklenen çıktılarla detaylandırılmıştır.

---

## 🎯 Geliştirme Aşamaları

### Aşama 1: Proje Kurulumu ve Temel Altyapı

#### 1.1 Proje Yapısını Oluştur
```
PhotoNext/
├── app/
│   ├── src/main/
│   │   ├── java/com/photonext/app/
│   │   │   ├── PhotoNextApplication.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── di/
│   │   │   │   ├── AppModule.kt
│   │   │   │   ├── RepositoryModule.kt
│   │   │   │   └── UseCaseModule.kt
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── PhotoDatabase.kt
│   │   │   │   │   ├── dao/
│   │   │   │   │   │   ├── PhotoDao.kt
│   │   │   │   │   │   └── EditHistoryDao.kt
│   │   │   │   │   └── entity/
│   │   │   │   │       ├── PhotoEntity.kt
│   │   │   │   │       └── EditHistoryEntity.kt
│   │   │   │   ├── model/
│   │   │   │   │   ├── Photo.kt
│   │   │   │   │   ├── EditOperation.kt
│   │   │   │   │   └── EditState.kt
│   │   │   │   └── repository/
│   │   │   │       ├── PhotoRepository.kt
│   │   │   │       └── EditHistoryRepository.kt
│   │   │   ├── domain/
│   │   │   │   ├── usecase/
│   │   │   │   │   ├── LoadPhotoUseCase.kt
│   │   │   │   │   ├── ApplyEditUseCase.kt
│   │   │   │   │   ├── UndoEditUseCase.kt
│   │   │   │   │   ├── RedoEditUseCase.kt
│   │   │   │   │   └── SavePhotoUseCase.kt
│   │   │   │   └── model/
│   │   │   │       └── EditResult.kt
│   │   │   ├── presentation/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   │   ├── EditScreen.kt
│   │   │   │   │   │   ├── ExportScreen.kt
│   │   │   │   │   │   └── GalleryScreen.kt
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── EditControls.kt
│   │   │   │   │   │   ├── SliderControl.kt
│   │   │   │   │   │   ├── PhotoPreview.kt
│   │   │   │   │   │   └── BottomSheet.kt
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       ├── Theme.kt
│   │   │   │   │       └── Type.kt
│   │   │   │   └── viewmodel/
│   │   │   │       ├── HomeViewModel.kt
│   │   │   │       ├── EditViewModel.kt
│   │   │   │       └── ExportViewModel.kt
│   │   │   └── util/
│   │   │       ├── ImageProcessor.kt
│   │   │       ├── ColorMatrixFactory.kt
│   │   │       ├── BitmapLoader.kt
│   │   │       ├── BitmapCache.kt
│   │   │       ├── EditHistoryManager.kt
│   │   │       ├── PermissionManager.kt
│   │   │       └── Constants.kt
│   │   └── res/
│   │       ├── drawable/
│   │       ├── values/
│   │       │   ├── colors.xml
│   │       │   ├── strings.xml
│   │       │   └── themes.xml
│   │       └── xml/
│   │           └── file_paths.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

#### 1.2 Gradle Yapılandırması
- **Root build.gradle.kts:**
  - Kotlin version: 1.9.20
  - Gradle version: 8.2
  - Compose compiler version: 1.5.4

- **App build.gradle.kts:**
  - Min SDK: 29
  - Target SDK: 34
  - Dependencies:
    - Jetpack Compose BOM
    - Hilt
    - Room
    - Coil
    - Navigation Compose
    - Coroutines
    - Accompanist (permissions)

#### 1.3 Temel Dosyalar
- **AndroidManifest.xml:** İzinler ve activity tanımları
- **PhotoNextApplication.kt:** Hilt application sınıfı
- **MainActivity.kt:** Ana activity ve navigation kurulumu

**Beklenen Çıktı:** Çalışan boş proje yapısı

---

### Aşama 2: Temel UI Bileşenleri

#### 2.1 Tema ve Renk Paleti
- Material Design 3 tema
- Dark/Light tema desteği
- Özel renk paleti (PhotoNext brand colors)

#### 2.2 Ana Ekran (HomeScreen)
- Logo ve başlık
- "Fotoğraf Seç" butonu
- "Kamera Aç" butonu
- Son düzenlenen fotoğraflar galerisi
- Settings menüsü

#### 2.3 Navigasyon Kurulumu
- Bottom navigation bar
- NavGraph tanımı
- Screen route'ları

**Beklenen Çıktı:** Çalışan UI ile ana ekran

---

### Aşama 3: Fotoğraf Yükleme ve Görüntüleme

#### 3.1 Fotoğraf Seçimi
- MediaStore entegrasyonu
- Photo Picker API (Android 14+)
- Galeri ekranı tasarımı
- Grid layout ile fotoğraf listesi

#### 3.2 Fotoğraf Yükleme
- BitmapLoader implementasyonu
- Memory-efficient loading
- Progress indicator
- Error handling

#### 3.3 Fotoğraf Görüntüleme
- AsyncImage ile görüntüleme
- Pinch to zoom
- Swipe gestures
- Full screen mod

**Beklenen Çıktı:** Fotoğraf seçme ve görüntüleme özelliği

---

### Aşama 4: Temel Düzenleme Araçları

#### 4.1 ImageProcessor Implementasyonu
```kotlin
class ImageProcessor {
    fun applyBrightness(bitmap: Bitmap, value: Float): Bitmap
    fun applyContrast(bitmap: Bitmap, value: Float): Bitmap
    fun applySaturation(bitmap: Bitmap, value: Float): Bitmap
    fun applyExposure(bitmap: Bitmap, value: Float): Bitmap
    fun applyTemperature(bitmap: Bitmap, value: Float): Bitmap
    fun applyTint(bitmap: Bitmap, value: Float): Bitmap
    fun applyHighlights(bitmap: Bitmap, value: Float): Bitmap
    fun applyShadows(bitmap: Bitmap, value: Float): Bitmap
}
```

#### 4.2 ColorMatrixFactory Implementasyonu
```kotlin
class ColorMatrixFactory {
    fun createBrightnessMatrix(value: Float): ColorMatrix
    fun createContrastMatrix(value: Float): ColorMatrix
    fun createSaturationMatrix(value: Float): ColorMatrix
    fun createExposureMatrix(value: Float): ColorMatrix
    fun createTemperatureMatrix(value: Float): ColorMatrix
    fun createTintMatrix(value: Float): ColorMatrix
    fun createHighlightsMatrix(value: Float): ColorMatrix
    fun createShadowsMatrix(value: Float): ColorMatrix
}
```

#### 4.3 EditControls UI
- Slider kontrol bileşeni
- Reset butonu
- Değer göstergesi
- Category tab'ları (Basic, Advanced)

#### 4.4 EditViewModel
- EditState yönetimi
- Slider değerlerini state'e bağlama
- Preview güncelleme

**Beklenen Çıktı:** Temel ayarlamalar ile çalışan düzenleme ekranı

---

### Aşama 5: Dönüşüm Araçları

#### 6.1 Rotate ve Flip
```kotlin
fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap
fun flipBitmapHorizontal(bitmap: Bitmap): Bitmap
fun flipBitmapVertical(bitmap: Bitmap): Bitmap
```

#### 6.2 Crop Tool
- Crop overlay UI
- Aspect ratio seçenekleri (Free, 1:1, 4:3, 16:9, 9:16)
- Drag handles
- Crop preview

#### 6.3 Straighten
- Rotation slider (-45° to +45°)
- Grid overlay
- Auto-straighten (ileri ekle)

**Beklenen Çıktı:** Dönüşüm araçları

---

### Aşama 6: Undo/Redo Sistemi

#### 6.1 EditHistoryManager
```kotlin
class EditHistoryManager {
    private val history = mutableListOf<EditOperation>()
    private var currentIndex = -1
    
    fun addOperation(operation: EditOperation)
    fun undo(): EditOperation?
    fun redo(): EditOperation?
    fun canUndo(): Boolean
    fun canRedo(): Boolean
    fun clear()
    fun getCurrentState(): EditState
}
```

#### 6.2 EditOperation Sealed Class
```kotlin
sealed class EditOperation {
    data class Brightness(val value: Float) : EditOperation()
    data class Contrast(val value: Float) : EditOperation()
    data class Saturation(val value: Float) : EditOperation()
    data class Exposure(val value: Float) : EditOperation()
    data class Temperature(val value: Float) : EditOperation()
    data class Tint(val value: Float) : EditOperation()
    data class Highlights(val value: Float) : EditOperation()
    data class Shadows(val value: Float) : EditOperation()
    data class Rotate(val degrees: Int) : EditOperation()
    data class Flip(val horizontal: Boolean, val vertical: Boolean) : EditOperation()
    data class Crop(val rect: Rect) : EditOperation()
}
```

#### 6.3 Undo/Redo UI
- Undo butonu (toolbar)
- Redo butonu (toolbar)
- Keyboard shortcuts (ileri ekle)
- History dialog

**Beklenen Çıktı:** Çalışan undo/redo sistemi

---

### Aşama 7: Metin ve Çizim

#### 7.1 Text Overlay
- Text input dialog
- Font seçimi
- Color picker
- Size slider
- Position drag
- Rotation

#### 7.2 Drawing
- Drawing canvas
- Brush size slider
- Color picker
- Eraser tool
- Undo for drawing

**Beklenen Çıktı:** Metin ve çizim araçları

---

### Aşama 8: Kaydetme ve Export

#### 8.1 Export Seçenekleri
- Format seçimi (JPEG, PNG, WEBP)
- Quality slider (0-100)
- Resolution seçimi (Original, Medium, Small)
- Metadata seçenekleri (EXIF koru/kaldır)

#### 8.2 Save İşlemi
- Orijinal üzerine kaydet
- Yeni kopya olarak kaydet
- Progress indicator
- Success/error feedback

#### 8.3 Paylaşım
- Share intent
- Sosyal medya entegrasyonu
- Direct share targets

**Beklenen Çıktı:** Kaydetme ve paylaşım özellikleri

---

### Aşama 9: UI/UX İyileştirmeleri

#### 9.1 Animasyonlar
- Screen transitions
- Button press animations
- Slider value change animations
- Loading animations

#### 9.2 Jest Desteği
- Pinch to zoom
- Double tap to reset
- Swipe to dismiss
- Long press for options

#### 9.3 Before/After Karşılaştırma
- Split view
- Tap and hold comparison
- Smooth transition

#### 9.4 Performans Optimizasyonu
- Bitmap caching
- Debounce for slider updates
- Lazy loading
- Memory management

**Beklenen Çıktı:** İyileştirilmiş UI/UX

---

### Aşama 10: Test ve Hata Ayıklama

#### 10.1 Unit Tests
- ImageProcessor tests
- ColorMatrixFactory tests
- EditHistoryManager tests
- Use case tests

#### 10.2 Integration Tests
- Repository tests
- ViewModel tests
- UI component tests

#### 10.3 UI Tests
- Screen navigation tests
- Edit operation tests
- Save/export tests

#### 10.4 Hata Ayıklama
- Crash reporting
- Error logging
- User feedback collection

**Beklenen Çıktı:** Stabil, test edilmiş uygulama

---

### Aşama 11: Final İyileştirmeler

#### 11.1 Dokümantasyon
- README.md
- User guide
- API documentation (ileri ekle)

#### 11.2 Code Review
- Code quality check
- Performance review
- Security review

#### 11.3 Release Hazırlığı
- Version bump
- Release notes
- APK generation
- Signing

**Beklenen Çıktı:** Production-ready uygulama

---

## 📊 Görev Önceliklendirme

### Yüksek Öncelik (MVP)
1. ✅ Proje kurulumu
2. ✅ Temel UI
3. ✅ Fotoğraf yükleme
4. ✅ Temel ayarlamalar (brightness, contrast, saturation)
5. ✅ Undo/Redo
6. ✅ Kaydetme

### Orta Öncelik
1. Dönüşüm araçları (rotate, flip, crop)
2. Before/After karşılaştırma
3. Export seçenekleri

### Düşük Öncelik (İleri ekle)
1. Metin ve çizim
2. Batch processing
3. AI-powered editing

---

## 🎯 Başarı Kriterleri

### Fonksiyonel Gereksinimler
- [ ] Fotoğraf yükleme ve görüntüleme
- [ ] Temel ayarlamalar (brightness, contrast, saturation)
- [ ] Rotate ve flip
- [ ] Undo/Redo
- [ ] Kaydetme (JPEG, PNG)
- [ ] Paylaşım

### Performans Gereksinimleri
- [ ] Düzenleme işlemleri < 100ms
- [ ] 4K fotoğraflarda akıcı çalışma
- [ ] Memory kullanımı < 200MB
- [ ] APK boyutu < 50MB

### Kullanıcı Deneyimi
- [ ] < 3 tıklama ile temel düzenleme
- [ ] Intuitive UI
- [ ] Smooth animations
- [ ] Clear feedback

---

## 📝 Notlar

### Teknik Kararlar
1. **Jetpack Compose:** Modern, declarative UI
2. **Hilt:** Dependency injection
3. **Room:** Local database (ileri ekle)
4. **Coil:** Image loading
5. **Coroutines:** Asenkron işlemler

### Riskler ve Mitigasyon
1. **Memory Issues:** Bitmap cache ve efficient loading
2. **Performance:** Debounce ve background processing
3. **Compatibility:** Scoped storage uyumluluğu

### Bağımlılıklar
- Android Studio Hedgehog+
- JDK 17
- Android SDK 34
- Gradle 8.2+

---

**Sonraki Adım:** Aşama 1'den başlayarak projeyi oluşturmaya başlayın.
