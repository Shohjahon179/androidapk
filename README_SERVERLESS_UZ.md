# Vagon Control — lokal (serversiz) variant

## Kiritilgan o'zgarishlar
- Server/bulut sinxronizatsiyasi majburan o'chirilgan: ma'lumotlar telefonning ichida saqlanadi.
- Ro'yxatdan o'tgach hudud → tuman/shahar → stansiya/vokzal tanlash oqimi.
- Xorazm viloyati alohida hudud; uning stansiyalari Qo'ng'irot temir yo'l uzeli ma'lumotlaridan olinadi.
- Admin paneldagi Karta/sozlamalar bo'limida bepul sinov kunini o'zgartirish mumkin (dastlab 7 kun).
- Chat xabarlari tanlangan viloyat va tuman/shahar bo'yicha filtrlanadi.
- Chat yozish oynasiga tezkor emoji/stiker tugmalari qo'shildi.
- GitHub Actions workflow loyihaning `settings.gradle.kts` faylini topib, Gradle 8.6 bilan APK yig'adi.

## Muhim cheklov
Bu haqiqiy serversiz rejim. Har bir telefon o'zining lokal ma'lumotlar bazasiga ega:
- boshqa telefonda ro'yxatdan o'tgan foydalanuvchi bu telefondagi admin panelda ko'rinmaydi;
- chat xabarlari telefonlar orasida uzatilmaydi;
- admin sinov muddatini faqat o'zi kirgan telefondagi foydalanuvchilar uchun o'zgartiradi;
- ilovani o'chirish yoki telefon ma'lumotlarini tozalash lokal ma'lumotlarni yo'qotishi mumkin.

## APK yig'ish
GitHub repository'ga ZIP ichidagi fayllarni to'g'ridan-to'g'ri repository ildiziga yuklang. So'ng Actions → Build APK → Run workflow.
Artifact: `Vagon-Control-Serverless-APK`.
