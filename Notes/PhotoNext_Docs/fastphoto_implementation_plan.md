# FastPhoto - Implementation Plan & Bug Fixes

Aşağıdaki liste, S24 Ultra testlerinizde karşılaştığınız tüm sorunların temel sebeplerini ve uygulayacağımız kesin çözümleri adım adım göstermektedir. Projenin ana dili bundan sonra sadece **İngilizce** olacaktır.

## Phase 1: Core System & Permissions (Kök Nedenlerin Çözümü)
**Sorun:** Silinen resimlerin galeride kalması (Silinememesi) ve taşıma yaparken (Move) eski dosyanın silinemeyip kopyasının oluşması.
**Kök Neden:** Android 11+ sisteminin bizi güvenlik sebebiyle engellemesi ve hataları sessizce yutması.
1. `MainActivity.kt` dosyasına `MANAGE_EXTERNAL_STORAGE` (Tüm dosyalara erişim) izni isteme ekranı eklenecek. Uygulamaya ilk girişte bu tam yetkiyi vereceksiniz.
2. Bu yetki sayesinde `MediaRepository.kt` ve `TrashRepository.kt` dosyalarındaki "Kopyala ve Eski Yeri Sil" işlemleri işletim sistemine takılmadan %100 başarıyla çalışacak. Orijinal galerinide "hayalet" kopyalar kalmayacak.

## Phase 2: Album Merging & File Paths (Klasör Karmaşası)
**Sorun:** Samsung Galeride "Aynı isimli albümler birleştirildi" uyarısı çıkması.
**Kök Neden:** Taşıma yaparken kodumuz dosyayı sabit olarak `Pictures/KlasörAdı` yoluna atıyor. Eğer hedef klasör `DCIM/KlasörAdı` içindeyse, sistem aynı isimde 2 farklı klasör görüp kafası karışıyor.
1. `MediaRepository.kt` güncellenecek. Hedef klasörün "Gerçek dosya yolu (Relative Path)" bulunup dosya tam olarak o adrese taşınacak. Sabit yol kullanılmayacak.

## Phase 3: UI & UX Fixes (Arayüz Hataları)
1. **Refresh Issue (Ekranda Kalma):** Kaydırılan (taşınan/silinen) resim ekrandan anında yok olacak şekilde Pager/State yapısı güncellenecek.
2. **Ghosting (Arka plan izi):** `Coil` kütüphanesinin crossfade özelliği kapatılıp, sayfalar arası geçişlerde önceki resimlerin iz bırakması (overlap) engellenecek.
3. **Status Bar (Mavi Şerit):** Uygulamanın en üstündeki mavi şerit tamamen Siyah (Black) yapılacak, saat ve şarj ikonları beyaz (Light Status Bar) olacak.
4. **Trash Badge (Çöp Tenekesi Taşıması):** Çöp ikonunun üzerindeki sayı (Badge) tam ortalanacak ve ekrandan taşması engellenecek.
5. **English Only:** Uygulamadaki tüm metinler (Geri al, Tüm fotoğraflar vb.) sadece **İngilizce** yapılacak.

## Phase 4: Feature Enhancements (Yeni Geliştirmeler)
1. **Dynamic Recent Folders:** Ekranın altındaki klasör barı artık rastgele değil, hafızaya alınan "Son kullanılan (Move yapılan) 5 klasörü" gösterecek.
2. **Sort by Date:** Fotoğraflar karışık değil, `DATE_TAKEN` (Çekilme Tarihi) verisine göre en yeniden eskiye doğru sıralanacak.
3. **New Folder Creation:** Klasör seçme (Folder Picker) ekranına `[+ New Folder]` butonu eklenecek.
4. **Nested Folders (İç İçe Klasörler):** Eğer klasörler hiyerarşikse (Örn: `WhatsApp/Images`), listede bu kırılımlarıyla beraber (Ağaç yapısı) gösterilecek.
