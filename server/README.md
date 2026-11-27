# Vagon server (PostgreSQL)

Muhit o'zgaruvchilari:
- DATABASE_URL  — Postgres ulanish manzili (postgres://user:pass@host:5432/db)
- API_KEY       — ilovadagi DEFAULT_API_KEY bilan bir xil bo'lishi shart
- PGSSL=disable — faqat lokal Postgres uchun

Lokal sinash:
  cd server && npm install
  DATABASE_URL=postgres://postgres:parol@localhost:5432/vagon API_KEY=kalit PGSSL=disable npm start
  curl -H "X-API-Key: kalit" http://localhost:3000/api/users

## v12: to'lov tizimi uchun yangi jadvallar
Server ishga tushganda avtomatik yaratiladi (qo'lda hech narsa qilish shart emas):
- payments  — yuborilgan cheklar (holati: pending / approved / rejected)
- settings  — admin qo'shgan karta, narx (10 000), sinov (7 kun), to'lov muddati (30 kun)
- receipts  — chek rasmlari (base64 JPEG)
- GET /api/time — server vaqti (sinov muddati shu vaqt bo'yicha hisoblanadi)
