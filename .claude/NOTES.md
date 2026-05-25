# FastPhoto — Uzun Form Notlar & Karar Geçmişi

> Bu dosya proje-spesifik kararların, teknik nüansların ve sürüm geçmişinin referans deposudur. CLAUDE.md kısa talimat, bu dosya derin context.

---

## 1. Sürüm Geçmişi

### v1.0.0 — İlk release
- Tinder-tarzı swipe (yukarı=sil, sağ-sol=nav)
- Custom in-app trash (Room DB)
- Bulk delete: tek sistem dialogu ile birden fazla foto silme

### v1.1.0 — DCIM fix + EN + pending button + auto-publish
- **DCIM/Pictures path bug fix**: Move ederken yeni klasör `Pictures/Camera/` altında açılıyordu (hardcoded prefix). `Album.relativePath` field'i eklendi, copy normalize ediliyor; blank ise default `DCIM/<name>/`.
- **Pending Commit Button**: Top bar'da, sarı saat / yeşil tik. Bekleyen değişiklik sayısı rozet olarak. Tıklayınca tek sistem dialogu ile hepsi onaya gönderiliyor.
- **English-only UI**: strings.xml + inline string'ler tamamen İngilizce.
- **GitHub Releases auto-publish**: `softprops/action-gh-release@v2` ile `latest` tag'ine stable URL.

### v1.1.1 — Bundled UX fixes (mevcut)
5 sorun tek commit:
1. Move sonrası DATE_TAKEN/DATE_ADDED/DATE_MODIFIED kopyalama (foto kronolojik yerinde kalsın)
2. `BadgedBox`+`Badge` → custom yuvarlak Box (sayı içinde, taşma yok)
3. Swipe stack arkaplanı solid black (next photo letterbox sızıntısı yok)
4. Move picker'da hierarchical gruplama (DCIM/Pictures expandable parent'lar)
5. "+ New Folder" entry'si (DCIM altına yeni klasör + foto move tek akış)

---

## 2. Tasarım Kararları — Derin

### 2.1 Move pattern: Copy + Hide + Bulk Commit
**Karar**: Cross-bucket move için **bayt-bayt kopya yap → orijinali Room'a "trash" olarak ekle → kullanıcı commit'lediğinde tüm trashleri tek sistem dialogu ile sil.**

**Alternatif (REDDEDİLDİ)**: `MediaStore.createWriteRequest` + `RELATIVE_PATH` update.

**Neden mevcut yaklaşım?**

| Boyut | Copy+Hide+Commit | createWriteRequest |
|---|---|---|
| Sistem dialogu | Tek (commit anı, tüm değişiklikler için) | Her foto için ayrı |
| Disk maliyeti | Geçici bayt kopyası | Yok (gerçek rename) |
| Swipe UX | Akıcı, kesintisiz | Her swipe sonrası blok |
| Veri kaybı riski | Düşük (kopya hazır, orijinal hala disk'te) | Düşük |
| Atomic ROLLBACK | Kolay (kopya silinir) | Mümkün ama tek per-photo dialog |

S24 Ultra'da disk yazma maliyeti milisaniye seviyesinde; kullanıcı tek bulk dialogu tercih ediyor. Bu kararı tartışma yarısı kaldırma. (Kullanıcı v1.1.1 plan'ında doğruladı: "yapalım [hierarchical+new folder]" demeden önce "daha temiz bir yöntem yok mu?" diye sordu, açıklayınca kabul etti.)

### 2.2 DATE_TAKEN/DATE_ADDED/DATE_MODIFIED açık set
**Sorun**: `copyPhotoToAlbum` insert'inde tarih kolonları boş bırakılırsa, Android `now` ile doldurur. Galeri sort `DATE_TAKEN DESC, DATE_ADDED DESC` olduğu için move edilmiş foto en üste fırlar.

**Çözüm** ([MediaRepository.kt:202-204](../app/src/main/java/com/fastphoto/app/data/repository/MediaRepository.kt#L202-L204)):
```kotlin
photo.dateTaken?.let { put(MediaStore.Images.Media.DATE_TAKEN, it) }
put(MediaStore.Images.Media.DATE_ADDED, photo.dateAdded)
put(MediaStore.Images.Media.DATE_MODIFIED, photo.dateAdded)
```

**Photo modeli convention'ı** (kaynağa güven, override etme):
- `dateTaken: Long?` — **milisaniye**
- `dateAdded: Long` — **saniye** (Android MediaStore convention)

EXIF zaten bayt-bayt kopya ile intact, ama MediaStore kolonlarına explicit set lazım. EXIF parse her Android sürümünde güvenilir değil.

### 2.3 In-icon circular badges
**Sorun**: Material3 `BadgedBox + Badge` rozet'i icon'un sağ-üst köşesine overlay yapıyor; çok haneli sayıda (örn. "16+") ekranın sağına taşıyor. `IconButton` 48dp safe-zone'unu aşıyor.

**Çözüm**: 40dp `Box` + `clip(CircleShape)` + `background(<color>)`. Rakam yuvarlağın **içinde**. Yuvarlak = ikon = clickable area. Overflow fiziksel olarak imkânsız.

**Renkler**:
- Trash: kırmızı `Color(0xFFE53935)` (pending varken), translucent beyaz `Color(0x66FFFFFF)` (boşken — orijinal trash ikonu)
- Pending: sarı `Color(0xFFFFC107)` (pending varken count içeride), yeşil `Color(0xFF4CAF50)` (clean → CheckCircle ikonu içeride)

### 2.4 Swipe stack siyah backdrop
**Sorun**: `PhotoSwipeStack` parent `Box`'unun arkaplanı yoktu. Hem current hem next photo `ContentScale.Fit` kullanıyor → aspect ratio uyuşmazsa letterbox şeffaf → arkadaki next photo current'ın letterbox bandlarından sızıyordu (özellikle landscape current + portrait next).

**Çözüm**: `Box(modifier = Modifier.fillMaxSize().background(Color.Black))`. Siyah backdrop next photo'yu maskeliyor; next photo yalnızca current sürüklenip çıkarken görünüyor (Tinder reveal davranışı).

### 2.5 Hierarchical move picker
**Sorun**: Kullanıcının ~100+ albümü var (DCIM/Camera, DCIM/Screenshots, Pictures/WhatsApp, Pictures/Telegram, etc.) Move picker'da hepsi flat liste → görsel kaos.

**Çözüm**: `Album.relativePath`'in ilk segment'ine göre grupla (DCIM, Pictures, Movies, ...). LazyColumn'da:
- Header row (parent adı + alt klasör sayısı + total foto sayısı + chevron) — clickable, toggle expand/collapse
- Expanded ise: indented child rows (sadece açık parent'lar render edilir, performance OK)

**Default expansion**: Mevcut foto'nun parent'ı + DCIM açık başlasın. State: `Set<String>`.

**Sort order**: DCIM en üstte, Pictures ikinci, sonra foto sayısına göre azalan.

### 2.6 "+ New Folder" akışı
**Karar**: Folder oluşturmadan önce klasörü disk'te mkdir yapma — gereksiz. MediaStore'a foto insert ederken `RELATIVE_PATH = "DCIM/$name/"` ile birlikte ilk yazımda klasör otomatik oluşur.

**Akış**:
1. Move picker'da "+ New Folder…" tıkla → AlertDialog
2. TextField'a isim yaz (validation: sadece trim, özel karakter kontrolü YOK — MediaStore zaten handle ediyor)
3. Confirm → `viewModel.moveToNewFolderByName(photo, name)` → `copyPhotoToAlbum(photo, "DCIM/$trimmed/")` → trash'e at
4. `loadAlbums()` reload → yeni bucket bir sonraki picker açılışında görünür

**Aynı isimli klasör varsa**: MediaStore içine yazıyor (silent merge). Bu kabul edilmiş davranış — kullanıcı bilerek yazıyor olabilir.

---

## 3. Teknik Notlar — MediaStore Quirks

### 3.1 IS_PENDING flag (Android Q+)
Insert'te `IS_PENDING = 1` set et, copy bitince `update(..., IS_PENDING = 0)` yap. Yoksa galeri uygulamaları yarım dosyayı gösterir.

### 3.2 RELATIVE_PATH normalize
Path **mutlaka trailing slash ile bitmeli**: `"DCIM/Camera/"` ✓, `"DCIM/Camera"` ✗. `copyPhotoToAlbum` içinde otomatik normalize ediliyor.

### 3.3 BUCKET_ID
Hash of normalized path, MediaStore tarafından otomatik üretilir. Aynı klasör için hep aynı ID döner. Albüm gruplaması için kullanılır.

### 3.4 Build.VERSION guard
`MediaStore.VOLUME_EXTERNAL` Android Q (API 29) ile geldi. `minSdk = 29` olduğu için aslında her yerde Q+ — ama kod hala defensive guard'lı (`if (SDK_INT >= Q)`).

### 3.5 ACCESS_MEDIA_LOCATION izni
EXIF GPS koordinatlarını koruyabilmek için gerekli. AndroidManifest'te tanımlı. Kullanıcıdan ayrı runtime izin gerekiyor.

---

## 4. UX Kuralları (kullanıcı tercihleri)

### 4.1 Swipe semantics
- **Yukarı (Up)**: SİL → animasyon yukarı kayma + trash'e ekle. Onay sorma.
- **Aşağı (Down)**: MOVE picker aç. Foto'yu yerine snap-back yapar (picker dismiss edilirse foto kalır).
- **Sol (Left)**: Sonraki foto. Yan etki YOK.
- **Sağ (Right)**: Önceki foto. Yan etki YOK.

### 4.2 Onay dialogları
- **Tek tek silmede onay YOK** — swipe direkt çalışsın. Geri almak için Undo (bottom-left, son 10 aksiyon).
- **Bulk commit'te tek onay** — pending button'a basınca tek sistem dialogu ile hepsi.
- **+ New Folder'da küçük dialog** — sadece isim sormak için, system dialogu değil.

### 4.3 Snackbar mesajları
- İngilizce, kısa, action sonrası feedback.
- Move sonrası: `"📁 Moved to <FolderName>"`
- Sil sonrası: `"Photo moved to trash"`
- Hata sonrası: `"<context>: <message>"` formatı.

### 4.4 Top bar renkleri (sabit)
- Background: `Color.Black.copy(alpha = 0.7f)`
- Text: `Color.White`
- Pending icon (sarı): `Color(0xFFFFC107)` — Material amber
- Trash icon (kırmızı): `Color(0xFFE53935)` — Material red 600
- Clean state (yeşil): `Color(0xFF4CAF50)` — Material green 500

---

## 5. Build & Deploy

### 5.1 Lokal build (Windows)
```bash
cd "c:/0.Software Big/FastPhoto"
./gradlew.bat assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk` (~6MB)

Süre: cold ~50s, incremental ~15s.

### 5.2 GitHub Actions
- Trigger: `push` to `main` veya `master`, ya da `workflow_dispatch`
- Job: `runs-on: ubuntu-latest`, JDK 17 temurin
- APK rename: `app-debug.apk` → `FastPhoto.apk`
- Release: tag `latest` (silinir + yeniden oluşturulur her push'ta)
- Stable URL: `https://github.com/onurataman/FastPhoto/releases/latest/download/FastPhoto.apk`
- Permission: `contents: write` (release publish için gerekli)

### 5.3 Lokal-CI senkronizasyon
Kullanıcı kuralı: **CI'a göndermeden önce lokal test.** Compile etmeyen kod push edilmez. Lokal build SUCCESSFUL gördükten sonra commit + push.

---

## 6. Bilinen Kapsam Dışı / TODO Adayları

- ❌ **`createWriteRequest` ile gerçek rename**: Reddedildi (UX gerilemesi). Bu kararı revize etme.
- ❌ **Klasör adı çakışma kontrolü**: MediaStore aynı isimli klasör varsa içine yazıyor. Kullanıcı bunu istiyor olabilir, validation eklenmedi.
- ❌ **Folder name özel karakter validation**: Sadece `trim()` + `trim('/')`. Diğer karakterler MediaStore'a bırakıldı.
- ❌ **EXIF location/lens info manuel kopya**: Bayt-bayt kopya zaten EXIF'i koruyor.
- ❌ **DATE_ADDED'ın bazı Android sürümlerinde override edilmesi**: Sistem davranışı, çözümü yok. Galeri uygulamasının kendi sort'una güveniyoruz.
- ⚠️ **Video desteği**: Şu an sadece `MediaStore.Images`. Video eklemek için `MediaStore.Files` veya ayrı `MediaStore.Video` projection gerekir.
- ⚠️ **Photo zoom/pan**: Yok. Şu an tek tap → controls toggle. Pinch zoom istenirse Coil + zoomable lib (örn. `me.saket.telephoto:zoomable`).

---

## 7. Sohbet Taşıma Notu

Bu klasörü (`c:/0.Software Big/FastPhoto/`) başka makineye veya yere kopyalarsan:
- ✅ Bu `.claude/` klasörü gelir — Claude sonraki sohbette tüm bağlamı buradan alır
- ✅ Git history gelir
- ❌ **Claude sohbet geçmişi GELMEZ** — `C:\Users\onura\.claude\projects\c--0-Software-Big\` altında, user home'da. Onu da elle taşımak istersen ayrı kopyala.

Bu dosyalar (`CLAUDE.md` + `NOTES.md`) sohbet history'sinin özetidir — yeni Claude oturumunda otomatik load olur ve önceki konuşmaların somut kararlarına/teknik nüanslarına erişebilir.
