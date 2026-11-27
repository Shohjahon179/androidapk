const express = require("express");
const { Pool } = require("pg");
const fs = require("fs");
const path = require("path");

const API_KEY = process.env.API_KEY || "";
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.PGSSL === "disable" ? false : { rejectUnauthorized: false },
});

const app = express();
app.use(express.json({ limit: "8mb" })); // chek rasmlari uchun

app.get("/health", (_req, res) => res.json({ ok: true }));

app.use("/api", (req, res, next) => {
  if (API_KEY && req.get("X-API-Key") !== API_KEY) {
    return res.status(401).json({ error: "API kalit noto'g'ri" });
  }
  next();
});

// Server vaqti: sinov muddatini telefon soatini o'zgartirib aldab bo'lmasligi uchun
app.get("/api/time", (_req, res) => res.json({ now: Date.now() }));

// Chek rasmlari (base64) — ro'yxat yengil bo'lishi uchun alohida jadval, faqat id bo'yicha olinadi
app.put("/api/receipts/:id", async (req, res) => {
  try {
    const image = req.body && req.body.image;
    if (typeof image !== "string" || !image) return res.status(400).json({ error: "image kerak" });
    await pool.query(
      `INSERT INTO receipts (id, image) VALUES ($1, $2)
       ON CONFLICT (id) DO UPDATE SET image = EXCLUDED.image`,
      [req.params.id, image]
    );
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.get("/api/receipts/:id", async (req, res) => {
  try {
    const r = await pool.query("SELECT image FROM receipts WHERE id = $1", [req.params.id]);
    if (r.rows.length === 0) return res.status(404).json({ error: "Chek topilmadi" });
    res.json({ id: req.params.id, image: r.rows[0].image });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

const TABLES = ["users", "records", "messages", "payments", "settings"];

for (const t of TABLES) {
  app.get(`/api/${t}`, async (_req, res) => {
    try {
      const r = await pool.query(`SELECT data FROM ${t} ORDER BY id`);
      res.json(r.rows.map((x) => x.data));
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });

  app.put(`/api/${t}/:id`, async (req, res) => {
    try {
      const data = { ...req.body, id: req.params.id };
      await pool.query(
        `INSERT INTO ${t} (id, data) VALUES ($1, $2)
         ON CONFLICT (id) DO UPDATE SET data = EXCLUDED.data`,
        [req.params.id, data]
      );
      res.json({ ok: true });
    } catch (e) {
      // users.username unique violation
      res.status(e.code === "23505" ? 409 : 500).json({ error: e.message });
    }
  });

  app.delete(`/api/${t}/:id`, async (req, res) => {
    try {
      await pool.query(`DELETE FROM ${t} WHERE id = $1`, [req.params.id]);
      res.json({ ok: true });
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });
}

(async () => {
  await pool.query(fs.readFileSync(path.join(__dirname, "schema.sql"), "utf8"));
  // Boshlang'ich akkauntlar (faqat jadval bo'sh bo'lsa)
  const c = await pool.query("SELECT count(*)::int AS n FROM users");
  if (c.rows[0].n === 0) {
    const seed = [
      { id: "1", phoneNumber: "+998901234567", username: "xodim", password: "1234", role: "Xodim" },
      { id: "2", phoneNumber: "+998907654321", username: "rahbar", password: "1234", role: "Rahbar" },
    ];
    for (const u of seed) await pool.query("INSERT INTO users (id,data) VALUES ($1,$2)", [u.id, u]);
  }
  // Karta/narx sozlamalari (admin ilovadan o'zgartiradi)
  const st = await pool.query("SELECT count(*)::int AS n FROM settings");
  if (st.rows[0].n === 0) {
    const def = { id: "payment", cardNumber: "", cardOwner: "", price: 10000, trialDays: 7, periodDays: 30 };
    await pool.query("INSERT INTO settings (id,data) VALUES ($1,$2)", ["payment", def]);
  }
  const port = process.env.PORT || 3000;
  app.listen(port, () => console.log("Vagon server :" + port));
})().catch((e) => { console.error(e); process.exit(1); });
