# FastPhoto — Proje Talimatları

Bu dosya sonraki Claude Code oturumlarında otomatik yüklenir. Detaylı history için [NOTES.md](NOTES.md) oku.

## Tek cümlede proje
Samsung S24 Ultra için Kotlin/Compose tabanlı, Tinder-tarzı swipe ile foto temizleme + albüm taşıma uygulaması. Tam offline, MediaStore API + Room DB, tek-tıklı bulk-approval pattern.

## Build & run (Windows / bash shell)
- Lokal build: `./gradlew.bat assembleDebug` (cwd: `c:/0.Software Big/FastPhoto/`)
- APK output: `app/build/outputs/apk/debug/app-debug.apk`
- Push to main → GitHub Actions otomatik build → `https://github.com/onurataman/FastPhoto/releases/latest/download/FastPhoto.apk` (stable URL)

## Mimari (kısa)
- **UI**: Jetpack Compose Material 3, tek ekran `PhotoViewerScreen.kt` (swipe stack + top bar + bottom chips)
- **State**: `MainPhotoViewModel` (Hilt-injected, reactive `StateFlow`)
- **Veri**: `MediaRepository` (MediaStore API), `TrashRepository` (Room DB), `PendingIntentBus` (Hilt singleton, IntentSender flow)
- **Optimistic UI**: `trashedIds: Set<Long>` ile mevcut liste filtrelenir; sistem silme ayrı (commit anında bulk)

## Sabit tasarım kararları (DEĞİŞTİRME)

| Karar | Neden |
|---|---|
| **Move = copy + hide-then-bulk-commit** (NOT `createWriteRequest`) | Single bulk approval dialog >> per-swipe dialog. Disk cost milisaniyeler. |
| **DCIM default** klasör parent'ı (Pictures değil) | S24 Ultra galeri uygulamalarının convention'ı. Pictures fotoğraflarla karışmıyor. |
| **MediaStore copy'de DATE_TAKEN/DATE_ADDED/DATE_MODIFIED set et** | Yoksa Android `now` yazar → sort DESC, foto en üste fırlar. |
| **In-icon circular badges** (BadgedBox değil) | Material3 Badge overlay'i taşma yapıyor (off-screen sağa). Circle IS the icon → fiziksel taşma imkânsız. |
| **Swipe stack arkaplanı solid black** | İki AsyncImage de ContentScale.Fit → letterbox bandlar şeffaf → arkadaki foto sızıyor. Black backdrop maskeliyor. |
| **Hierarchical move picker** (DCIM/Pictures expandable) | Onlarca albüm flat liste karman çorman; parent grouping UX kurtarıyor. |
| **English-only UI** | Kullanıcı tercihi. Türkçe string YAZMA (snackbar, dialog, hata mesajı dahil). |
| **Swipe semantic** | Yukarı=sil, aşağı=move picker, sağ/sol=navigation (yan-etki YOK). |

## Versiyon prosedürü
Her ship'te birlikte yap:
1. `app/build.gradle.kts` → `versionCode++` ve `versionName` bump (semver)
2. Commit mesajına versiyon yaz (örn. "v1.1.1 — ...")
3. Push'tan sonra GitHub Actions otomatik release oluşturur

## Branch stratejisi (kullanıcının tercihi)
- **Direkt main**: Tek-dosya UX fix, dokümantasyon, küçük rozet/renk
- **Branch + squash merge**: 3+ dosya değişikliği, yeni özellik, deney gerektiren iş

## Triage kuralı
- Bilinen sorun → direkt fix
- 2-3 kez tekrarlandıysa DUR: tüm sebepleri listele, kullanıcıdan onay al, sonra tek seferde fix et
- Tahminle kod yazma — önce teşhis
