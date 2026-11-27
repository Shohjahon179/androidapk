# Vagon Control — lokal (serversiz) variant

## Yangilangan imkoniyatlar
- Ro‘yxatdan o‘tishda viloyat/shahar → tuman → stansiya yoki vokzal tanlanadi.
- Xorazm viloyati alohida hudud sifatida ko‘rsatiladi; uning rasmiy stansiyalari Qo‘ng‘irot MTU ro‘yxatidan ajratiladi.
- O‘zbekiston temir yo‘llari AJning 2026-01-01 holatidagi 274 ta stansiya nomi loyihada saqlangan.
- Rasmiy ro‘yxatda yo‘lovchi tashishga oid deb belgilangan vokzal/stansiyalar alohida bo‘limda ko‘rsatiladi: Toshkent-yo‘lovchi, Toshkent-Janubiy, Kattaqo‘rg‘on-yo‘lovchi va Xiva.
- Tumanlar ro‘yxati viloyatlar bo‘yicha kengaytirildi. Tuman bilan aniq bog‘lanishi rasmiy ro‘yxatda ko‘rsatilmagan stansiyalar hududning umumiy stansiyalar ro‘yxatida chiqishi mumkin.
- Admin panelidan qo‘shimcha admin login/paroli yaratish mumkin.
- Admin panelidagi karta/sozlamalar bo‘limidan bepul sinov kunlari (standart 7 kun) o‘zgartiriladi.
- Chat xabarlari tanlangan viloyat va tuman/shahar bo‘yicha filtrlanadi.
- Xabar yozish oynasida stiker/emoji paneli tugmasi qo‘shildi.
- GitHub Actions workflow Gradle 8.6 bilan APK yig‘adi.

## Muhim cheklov
Bu haqiqiy serversiz rejim. Har bir telefon o‘zining lokal ma’lumotlariga ega:
- boshqa telefonda ro‘yxatdan o‘tgan foydalanuvchi bu telefondagi admin panelda ko‘rinmaydi;
- chat xabarlari telefonlar orasida uzatilmaydi;
- admin qo‘shish, sinov muddatini boshqarish va foydalanuvchilar ro‘yxati faqat shu telefonda amal qiladi;
- ilovani o‘chirish yoki telefon ma’lumotlarini tozalash lokal ma’lumotlarni yo‘qotishi mumkin.

## Manba
Stansiya nomlari «O‘zbekiston temir yo‘llari» AJning 2026-yil 1-yanvar holatidagi rasmiy stansiyalar ro‘yxatiga tayangan:
https://railway.uz/uz/uslugi/gruzovye_perevozki/223/

## APK yig‘ish
ZIP ichidagi fayllarni GitHub repository ildiziga yuklang. So‘ng Actions → Build APK → Run workflow.
Artifact: `Vagon-Control-Serverless-APK`.


## Premium muddati eslatmasi
- Profil sahifasida sinov/Premium tugash sanasi va qolgan kunlar ko'rsatiladi.
- Android bildirishnomasi muddati tugashidan 1 kun oldin rejalashtiriladi.
- Android 13 va undan yangi versiyalarda bildirishnomaga ruxsat berish kerak.
- Telefon o'chirilgan yoki ilova bildirishnomalari bloklangan bo'lsa, eslatma ko'rinmasligi mumkin. Ilova qayta ochilganda eslatma qayta rejalashtiriladi.
