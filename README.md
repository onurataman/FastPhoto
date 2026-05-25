# FastPhoto (S24 Ultra Edition) 🚀

Samsung Galaxy S24 Ultra gibi yüksek performanslı Android cihazlar için özel olarak tasarlanmış, "Işık Hızında Fotoğraf Organizasyon ve Temizlik" galerisi.

## 🌟 Yeni Nesil Özellikler (UX)
- **Aşağı Kaydırarak Taşıma (Swipe Down to Move):** Fotoğrafı aşağı kaydırdığınızda, ekranın altındaki "Son Kullanılan Klasörler" barında seçili olan hedef klasöre tek hamlede (1 saniye) taşınır.
- **Yukarı Kaydırarak Silme (Swipe Up to Delete):** Fotoğrafı yukarı fırlattığınızda anında işletim sisteminin Çöp Kutusuna (Trash) gider. Sıfır bekleme, sıfır "Emin misiniz?" onay sorusu. (Kök erişimi sayesinde).
- **Zaman Makinesi (Undo Stack):** Çok mu hızlı kaydırdınız? Yanlışlıkla sildiniz mi? Sol altta beliren **Geri Al (Undo)** butonuna basarak son 10 işleminizi milisaniyeler içinde geri alıp fotoğrafı eski yerine getirebilirsiniz.
- **Akıllı Klasör Barı:** Sürekli klasör seçmekle uğraşmazsınız. Alt taraftaki yatay kaydırılabilir bar, hedef klasörlerinizi her zaman başparmağınızın altında hazır tutar.

## 🛠 Teknik Mimari
- **Dil:** %100 Kotlin
- **Arayüz:** Jetpack Compose (120Hz ekranlar için LazyRow ve Donanımsal Animasyonlar)
- **Mimari:** MVVM, Coroutines (Asenkron İşlemler)
- **Depolama:** MediaStore API, Room Database (Geri al hafızası için)

## 📦 Kurulum ve Derleme (GitHub Actions)
Bilgisayarınızda hiçbir kod derleme aracı kurmanıza gerek kalmadan doğrudan buluttan APK alabilirsiniz.
1. GitHub deponuzdaki **Actions** sekmesine gidin.
2. Sol menüden **"Build FastPhoto APK"** seçeneğine tıklayın.
3. Sağdaki **"Run workflow"** butonuna basın.
4. İşlem 3-4 dakika sürecek. Tamamlandığında aşağıdan **`FastPhoto-Debug-APK.zip`** dosyasını indirin.
5. İçinden çıkan `.apk` dosyasını telefonunuza atın ve kurun.
