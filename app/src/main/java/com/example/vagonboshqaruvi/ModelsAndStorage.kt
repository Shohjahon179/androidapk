package com.example.vagonboshqaruvi

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class User(
    val id: String = "",
    val phoneNumber: String = "",
    val username: String = "",
    val password: String = "",
    val role: String = "Tanlanmagan",
    val region: String = "",
    val district: String = "",
    val station: String = "",
    val trialStart: Long = 0L,
    val paidUntil: Long = 0L
)

data class WagonRecord(
    val id: String = "",
    val trackNumber: String = "1-yo'l",
    val wagonNumber: String = "",
    val arrivalDate: String = "",
    val arrivalTime: String = "",
    val trainInNumber: String = "",
    val givenToUnloadDate: String = "",
    val givenToUnloadTime: String = "",
    val takenFromUnloadDate: String = "",
    val takenFromUnloadTime: String = "",
    val departureDate: String = "",
    val departureTime: String = "",
    val trainOutNumber: String = "",
    val durationUntilGiven: String = "",
    val durationAfterTaken: String = "",
    val totalDuration: String = "",
    val createdBy: String = ""
)

data class ChatMessage(
    val id: String = "",
    val senderName: String = "",
    val senderRole: String = "",
    val text: String = "",
    val time: String = "",
    val isEdited: Boolean = false,
    val fileName: String? = null,
    val localFilePath: String? = null,
    val region: String = "",
    val district: String = ""
)

// Admin qo'shadigan karta va narx (serverda "settings" jadvalida, id = "payment")
data class PaymentSettings(
    val id: String = "payment",
    val cardNumber: String = "",
    val cardOwner: String = "",
    val price: Int = 10000,
    val trialDays: Int = 7,
    val periodDays: Int = 30
)

// Foydalanuvchi yuborgan chek (rasmi alohida "receipts" jadvalida saqlanadi)
data class PaymentRequest(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val phoneNumber: String = "",
    val region: String = "",
    val district: String = "",
    val station: String = "",
    val amount: Int = 0,
    val status: String = "pending", // pending | approved | rejected
    val createdAt: Long = 0L,
    val decidedAt: Long = 0L
)

enum class AccessState { ADMIN, NOT_STARTED, TRIAL, PAID, EXPIRED }

data class AccessInfo(val state: AccessState, val daysLeft: Int = 0)

object Subscription {
    const val DAY_MS = 24L * 60 * 60 * 1000

    private fun daysLeft(ms: Long): Int = ((ms + DAY_MS - 1) / DAY_MS).toInt()

    fun access(user: User, now: Long, s: PaymentSettings): AccessInfo {
        if (user.role == "Admin") return AccessInfo(AccessState.ADMIN)
        if (user.paidUntil > now) return AccessInfo(AccessState.PAID, daysLeft(user.paidUntil - now))
        if (user.trialStart == 0L) {
            // Sinov hech boshlanmagan: to'lov muddati ham tugagan bo'lsa qayta bepul sinov berilmaydi
            return if (user.paidUntil > 0L) AccessInfo(AccessState.EXPIRED) else AccessInfo(AccessState.NOT_STARTED)
        }
        val trialEnd = user.trialStart + s.trialDays * DAY_MS
        return if (trialEnd > now) AccessInfo(AccessState.TRIAL, daysLeft(trialEnd - now))
        else AccessInfo(AccessState.EXPIRED)
    }
}

fun formatMoney(n: Int): String = String.format(Locale.US, "%,d", n).replace(',', ' ')

object TimeCalculator {
    private val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    fun calculateDifference(date1: String, time1: String, date2: String, time2: String): String {
        if (date1.isBlank() || time1.isBlank() || date2.isBlank() || time2.isBlank()) {
            return "Kiritilmagan"
        }
        return try {
            val d1 = format.parse("${date1.trim()} ${time1.trim()}") ?: return "Xato sana/vaqt"
            val d2 = format.parse("${date2.trim()} ${time2.trim()}") ?: return "Xato sana/vaqt"
            val diffMillis = d2.time - d1.time
            if (diffMillis < 0) return "Vaqt xato (manfiy)"
            val totalMinutes = diffMillis / (1000 * 60)
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            "$hours soat $minutes daq"
        } catch (e: Exception) {
            "Format: kk.oo.yyyy va ss:dd"
        }
    }

    // "52 soat 10 daq" ko'rinishidagi natija 48 soatdan oshganmi?
    fun isOver48Hours(duration: String): Boolean {
        val m = Regex("""(\d+)\s*soat\s*(\d+)\s*daq""").find(duration) ?: return false
        val totalMinutes = m.groupValues[1].toLong() * 60 + m.groupValues[2].toLong()
        return totalMinutes > 48 * 60
    }

    fun splitWagonsAndCreateRecords(rawWagons: String, baseRecord: WagonRecord): List<WagonRecord> {
        val wagonList = rawWagons.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val untilGiven = calculateDifference(
            baseRecord.arrivalDate, baseRecord.arrivalTime,
            baseRecord.givenToUnloadDate, baseRecord.givenToUnloadTime
        )
        val afterTaken = calculateDifference(
            baseRecord.takenFromUnloadDate, baseRecord.takenFromUnloadTime,
            baseRecord.departureDate, baseRecord.departureTime
        )
        val total = calculateDifference(
            baseRecord.arrivalDate, baseRecord.arrivalTime,
            baseRecord.departureDate, baseRecord.departureTime
        )

        return wagonList.mapIndexed { index, wagonNum ->
            baseRecord.copy(
                id = "${System.currentTimeMillis()}_$index",
                trackNumber = "${index + 1}-yo'l",
                wagonNumber = wagonNum,
                durationUntilGiven = untilGiven,
                durationAfterTaken = afterTaken,
                totalDuration = total
            )
        }
    }
}

object ExcelExporter {
    private fun csvEscape(value: String): String {
        val clean = value.replace("\"", "\"")
        return "\"$clean\""
    }

    fun exportAndShareCsv(context: Context, records: List<WagonRecord>): String {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val file = File(dir, "Vagonlar_Jadvali_$stamp.csv")

        val headers = listOf(
            "№",
            "Yo'l",
            "Vagon №",
            "Kirgan sana",
            "Kirgan vaqt",
            "Kirish poyezd",
            "Tushirishga berilgan sana",
            "Berilgan vaqt",
            "Tushirishga berilgunga qadar turgan vaqt",
            "Yo'ldan olingan sana",
            "Olingan vaqt",
            "Jo'nagan sana",
            "Jo'nash vaqti",
            "Tushirish yo'lidan olingandan jo'nagunga qadar turgan vaqt",
            "Jo'nash poyezd",
            "Jami vaqt",
            "Kiritgan xodim"
        )

        val sb = StringBuilder()
        sb.append("\uFEFF")
        sb.append(headers.joinToString(";") { csvEscape(it) }).append("\n")

        records.forEachIndexed { index, r ->
            val row = listOf(
                "${index + 1}",
                r.trackNumber,
                r.wagonNumber,
                r.arrivalDate,
                r.arrivalTime,
                r.trainInNumber,
                r.givenToUnloadDate,
                r.givenToUnloadTime,
                r.durationUntilGiven,
                r.takenFromUnloadDate,
                r.takenFromUnloadTime,
                r.departureDate,
                r.departureTime,
                r.durationAfterTaken,
                r.trainOutNumber,
                r.totalDuration,
                r.createdBy
            )
            sb.append(row.joinToString(";") { csvEscape(it) }).append("\n")
        }

        file.writeText(sb.toString(), Charsets.UTF_8)

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Vagonlar harakati Excel jadvali")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Excel jadvalni yuklash / yuborish"))
        return file.name
    }
}

class LocalStorage(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("vagon_boshqaruvi_v8_db", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        const val DEFAULT_FIREBASE_URL = "https://SIZNING-SERVER.onrender.com"
        const val DEFAULT_API_KEY = "O'ZGARTIRING-API-KALIT"
    }

    // Server manzili kiritilmagan bo'lsa ilova BULUTSIZ (faqat shu telefonda) ishlaydi.
    // Profilda haqiqiy server manzili kiritilsa, bulut rejimi o'zi yoqiladi.
    fun isLocalMode(): Boolean = true

    // Bulutsiz rejimda chek rasmlari telefonning o'zida saqlanadi
    fun saveReceiptLocal(id: String, base64: String): Boolean {
        return try {
            val dir = File(appContext.filesDir, "receipts").apply { mkdirs() }
            File(dir, "$id.txt").writeText(base64)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun loadReceiptLocal(id: String): String? {
        return try {
            val f = File(File(appContext.filesDir, "receipts"), "$id.txt")
            if (f.exists()) f.readText() else null
        } catch (_: Exception) {
            null
        }
    }

    fun getFirebaseUrl(): String {
        return prefs.getString("server_url", DEFAULT_FIREBASE_URL) ?: DEFAULT_FIREBASE_URL
    }

    fun getApiKey(): String {
        return prefs.getString("api_key", DEFAULT_API_KEY) ?: DEFAULT_API_KEY
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString("api_key", key.trim()).apply()
    }

    fun saveFirebaseUrl(url: String) {
        val cleaned = url.trim().trimEnd('/')
        if (cleaned.isNotEmpty()) {
            prefs.edit().putString("server_url", cleaned).apply()
        }
    }

    fun pendingIds(kind: String): Set<String> {
        return prefs.getStringSet("pending_$kind", emptySet())?.toSet() ?: emptySet()
    }

    fun addPending(kind: String, id: String) {
        val set = pendingIds(kind).toMutableSet()
        set.add(id)
        prefs.edit().putStringSet("pending_$kind", set).apply()
    }

    fun removePending(kind: String, id: String) {
        val set = pendingIds(kind).toMutableSet()
        set.remove(id)
        prefs.edit().putStringSet("pending_$kind", set).apply()
    }

    fun saveSessionUserId(id: String) {
        prefs.edit().putString("session_user_id", id).apply()
    }

    fun loadSessionUserId(): String? = prefs.getString("session_user_id", null)

    // Har bir akkaunt uchun tanlangan lavozim shu telefonda ham eslab qolinadi
    fun saveRoleFor(userId: String, role: String) {
        prefs.edit().putString("role_$userId", role).apply()
    }

    fun loadRoleFor(userId: String): String? = prefs.getString("role_$userId", null)

    fun clearSession() {
        prefs.edit().remove("session_user_id").apply()
    }

    fun saveThemeMode(isNight: Boolean) {
        prefs.edit().putBoolean("is_night_glass", isNight).apply()
    }

    fun loadThemeMode(): Boolean {
        return prefs.getBoolean("is_night_glass", true)
    }

    fun saveUsers(list: List<User>) {
        prefs.edit().putString("users", gson.toJson(list)).apply()
    }

    fun loadUsers(): MutableList<User> {
        val json = prefs.getString("users", null) ?: return mutableListOf(
            User("1", "+998901234567", "xodim", "1234", "Xodim"),
            User("2", "+998907654321", "rahbar", "1234", "Rahbar")
        )
        val type = object : TypeToken<MutableList<User>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveRecords(list: List<WagonRecord>) {
        prefs.edit().putString("records", gson.toJson(list)).apply()
    }

    fun loadRecords(): MutableList<WagonRecord> {
        val json = prefs.getString("records", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<WagonRecord>>() {}.type
        return gson.fromJson(json, type)
    }

    fun savePayments(list: List<PaymentRequest>) {
        prefs.edit().putString("payments", gson.toJson(list)).apply()
    }

    fun loadPayments(): MutableList<PaymentRequest> {
        val json = prefs.getString("payments", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<PaymentRequest>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveSettings(s: PaymentSettings) {
        prefs.edit().putString("pay_settings", gson.toJson(s)).apply()
    }

    fun loadSettings(): PaymentSettings {
        val json = prefs.getString("pay_settings", null) ?: return PaymentSettings()
        return try { gson.fromJson(json, PaymentSettings::class.java) ?: PaymentSettings() } catch (_: Exception) { PaymentSettings() }
    }

    // Server vaqti bilan telefon vaqti orasidagi farq (sinov muddatini soatni o'zgartirib aldab bo'lmasligi uchun)
    fun saveTimeOffset(ms: Long) {
        prefs.edit().putLong("time_offset", ms).apply()
    }

    fun loadTimeOffset(): Long = prefs.getLong("time_offset", 0L)

    // Eski (bulutdan oldingi) telefon akkauntlarini bulutga bir marta ko'chirish belgisi
    fun isUsersMigrated(): Boolean = prefs.getBoolean("users_migrated_v12", false)

    fun setUsersMigrated() {
        prefs.edit().putBoolean("users_migrated_v12", true).apply()
    }

    fun saveMessages(list: List<ChatMessage>) {
        prefs.edit().putString("messages", gson.toJson(list)).apply()
    }

    fun loadMessages(): MutableList<ChatMessage> {
        val json = prefs.getString("messages", null) ?: return mutableListOf(
            ChatMessage("1", "Rahbar", "Rahbar", "Assalomu alaykum! Vagonlar harakatini nazoratga olamiz.", "09:00")
        )
        val type = object : TypeToken<MutableList<ChatMessage>>() {}.type
        return gson.fromJson(json, type)
    }
}

class CloudSyncManager(private val storage: LocalStorage) {
    private val gson = Gson()

    @Volatile
    var lastError: String = ""

    private fun baseUrl(): String = storage.getFirebaseUrl().trimEnd('/')

    private fun describeError(conn: HttpURLConnection, code: Int): String {
        val body = try {
            conn.errorStream?.bufferedReader()?.use { it.readText() }?.take(120) ?: ""
        } catch (_: Exception) {
            ""
        }
        return "HTTP $code $body".trim()
    }

    private suspend fun httpGet(path: String, timeoutMs: Int = 6000): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${baseUrl()}/api/$path")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("X-API-Key", storage.getApiKey())
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
            }
            val code = conn.responseCode
            if (code in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                lastError = ""
                text
            } else {
                lastError = describeError(conn, code)
                null
            }
        } catch (e: Exception) {
            lastError = e.message ?: "Tarmoq xatosi"
            null
        }
    }

    private suspend fun httpPut(path: String, jsonBody: String, timeoutMs: Int = 6000): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${baseUrl()}/api/$path")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                setRequestProperty("X-API-Key", storage.getApiKey())
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(jsonBody) }
            val code = conn.responseCode
            if (code in 200..299) {
                lastError = ""
                true
            } else {
                lastError = describeError(conn, code)
                false
            }
        } catch (e: Exception) {
            lastError = e.message ?: "Tarmoq xatosi"
            false
        }
    }

    private suspend fun httpDelete(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${baseUrl()}/api/$path")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("X-API-Key", storage.getApiKey())
                connectTimeout = 6000
                readTimeout = 6000
            }
            val code = conn.responseCode
            if (code in 200..299) {
                lastError = ""
                true
            } else {
                lastError = describeError(conn, code)
                false
            }
        } catch (e: Exception) {
            lastError = e.message ?: "Tarmoq xatosi"
            false
        }
    }

    suspend fun pushUser(user: User): Boolean {
        val safeKey = user.id.ifBlank { user.username }
        return httpPut("users/$safeKey", gson.toJson(user))
    }

    suspend fun fetchUsers(): List<User>? {
        val raw = httpGet("users") ?: return null
        if (raw == "null" || raw.isBlank()) return emptyList()
        return try {
            val listType = object : TypeToken<List<User>>() {}.type
            val list: List<User> = gson.fromJson(raw, listType)
            list
        } catch (_: Exception) {
            null
        }
    }

    suspend fun deleteUser(userId: String): Boolean {
        return httpDelete("users/$userId")
    }

    suspend fun fetchServerTime(): Long? {
        val raw = httpGet("time") ?: return null
        return try {
            val m = gson.fromJson(raw, Map::class.java)
            (m?.get("now") as? Number)?.toLong()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchPayments(): List<PaymentRequest>? {
        val raw = httpGet("payments") ?: return null
        if (raw == "null" || raw.isBlank()) return emptyList()
        return try {
            val listType = object : TypeToken<List<PaymentRequest>>() {}.type
            val list: List<PaymentRequest> = gson.fromJson(raw, listType)
            list.sortedBy { it.createdAt }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun pushPayment(p: PaymentRequest): Boolean {
        return httpPut("payments/${p.id}", gson.toJson(p))
    }

    suspend fun fetchSettings(): PaymentSettings? {
        val raw = httpGet("settings") ?: return null
        if (raw == "null" || raw.isBlank()) return PaymentSettings()
        return try {
            val listType = object : TypeToken<List<PaymentSettings>>() {}.type
            val list: List<PaymentSettings> = gson.fromJson(raw, listType)
            list.firstOrNull { it.id == "payment" } ?: PaymentSettings()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun pushSettings(s: PaymentSettings): Boolean {
        return httpPut("settings/payment", gson.toJson(s.copy(id = "payment")))
    }

    // Chek rasmi (base64 JPEG) alohida saqlanadi, ro'yxat yengil qolishi uchun
    suspend fun pushReceipt(id: String, base64: String): Boolean {
        return httpPut("receipts/$id", gson.toJson(mapOf("image" to base64)), timeoutMs = 40000)
    }

    suspend fun fetchReceipt(id: String): String? {
        val raw = httpGet("receipts/$id", timeoutMs = 40000) ?: return null
        return try {
            val m = gson.fromJson(raw, Map::class.java)
            m?.get("image") as? String
        } catch (_: Exception) {
            null
        }
    }

    suspend fun pushRecord(record: WagonRecord): Boolean {
        val safeKey = record.id.ifBlank { System.currentTimeMillis().toString() }
        return httpPut("records/$safeKey", gson.toJson(record))
    }

    suspend fun deleteRecord(recordId: String): Boolean {
        return httpDelete("records/$recordId")
    }

    suspend fun fetchRecords(): List<WagonRecord>? {
        val raw = httpGet("records") ?: return null
        if (raw == "null" || raw.isBlank()) return emptyList()
        return try {
            val listType = object : TypeToken<List<WagonRecord>>() {}.type
            val list: List<WagonRecord> = gson.fromJson(raw, listType)
            list.sortedBy { it.id }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun pushMessage(msg: ChatMessage): Boolean {
        val safeKey = msg.id.ifBlank { System.currentTimeMillis().toString() }
        return httpPut("messages/$safeKey", gson.toJson(msg))
    }

    suspend fun deleteMessage(msgId: String): Boolean {
        return httpDelete("messages/$msgId")
    }

    suspend fun fetchMessages(): List<ChatMessage>? {
        val raw = httpGet("messages") ?: return null
        if (raw == "null" || raw.isBlank()) return emptyList()
        return try {
            val listType = object : TypeToken<List<ChatMessage>>() {}.type
            val list: List<ChatMessage> = gson.fromJson(raw, listType)
            list.sortedBy { it.id }
        } catch (_: Exception) {
            null
        }
    }
}
