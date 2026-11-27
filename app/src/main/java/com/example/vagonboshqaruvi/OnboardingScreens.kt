package com.example.vagonboshqaruvi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

// ---------------------------------------------------------------------------
// Umumiy kichik komponentlar
// ---------------------------------------------------------------------------

@Composable
internal fun GoldButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val p = LocalGlassPalette.current
    val gradient = Brush.horizontalGradient(listOf(p.accentPrimary, p.accentSecondary, p.accentPrimary))
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth().height(52.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (enabled) 1f else 0.4f)
                .background(gradient, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = p.onAccentText)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = text,
                    color = p.onAccentText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
private fun ChoiceCard(title: String, onClick: () -> Unit) {
    val p = LocalGlassPalette.current
    Card(
        colors = CardDefaults.cardColors(containerColor = p.glassCardBg),
        border = BorderStroke(1.dp, p.glassBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = p.accentPrimary)
            Spacer(Modifier.width(12.dp))
            Text(title, color = p.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

private fun normalizeName(s: String): String =
    s.lowercase().replace(Regex("[ʻʼ’‘'`]"), "")

// ---------------------------------------------------------------------------
// 1) Shahar/viloyat -> stansiya yoki vokzal tanlash
// ---------------------------------------------------------------------------

@Composable
fun RegionStationScreen(
    username: String,
    onDone: (region: String, district: String, station: String) -> Unit,
    onLogout: () -> Unit
) {
    val p = LocalGlassPalette.current
    var region by remember { mutableStateOf<String?>(null) }
    var district by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    val districts = remember(region) {
        when (region) {
            "Xorazm viloyati" -> listOf(
                "Urganch shahri", "Hazorasp tumani", "Tuproqqal'a tumani", "Xonqa tumani",
                "Shovot tumani", "Bog‘ot tumani", "Xiva tumani", "Yangiariq tumani",
                "Gurlan tumani", "Qo‘shko‘pir tumani", "Yangibozor tumani", "Urganch tumani"
            )
            "Qoraqalpog‘iston Respublikasi" -> listOf(
                "Nukus shahri", "Amudaryo tumani", "Beruniy tumani", "Chimboy tumani",
                "Ellikqal'a tumani", "Kegeyli tumani", "Mo‘ynoq tumani", "Nukus tumani",
                "Qanliko‘l tumani", "Qo‘ng‘irot tumani", "Qorao‘zak tumani", "Taxtako‘pir tumani",
                "To‘rtko‘l tumani", "Xo‘jayli tumani", "Shumanay tumani",
                "Taxiatosh shahri", "Bo‘zatov tumani"
            )
            "Toshkent shahri" -> listOf(
                "Bektemir tumani", "Chilonzor tumani", "Mirobod tumani", "Mirzo Ulug‘bek tumani",
                "Olmazor tumani", "Sergeli tumani", "Shayxontohur tumani", "Uchtepa tumani",
                "Yakkasaroy tumani", "Yashnobod tumani", "Yunusobod tumani", "Yangihayot tumani"
            )
            "Toshkent viloyati" -> listOf(
                "Angren shahri", "Bekobod shahri", "Chirchiq shahri", "Olmaliq shahri",
                "Ohangaron shahri", "Yangiyo‘l shahri", "Nurafshon shahri", "Oqqo‘rg‘on tumani",
                "Ohangaron tumani", "Parkent tumani", "Piskent tumani", "Bo‘ka tumani",
                "Bo‘stonliq tumani", "Chinoz tumani", "Qibray tumani", "Quyi Chirchiq tumani",
                "O‘rta Chirchiq tumani", "Yuqori Chirchiq tumani", "Zangiota tumani",
                "Toshkent tumani", "Yangiyo‘l tumani", "Bekobod tumani", "O‘rtachirchiq tumani"
            )
            "Sirdaryo viloyati" -> listOf(
                "Guliston shahri", "Yangiyer shahri", "Shirin shahri", "Boyovut tumani",
                "Guliston tumani", "Mirzaobod tumani", "Oqoltin tumani", "Sayxunobod tumani",
                "Sardoba tumani", "Sirdaryo tumani", "Xovos tumani"
            )
            "Jizzax viloyati" -> listOf(
                "Jizzax shahri", "Arnasoy tumani", "Baxmal tumani", "Do‘stlik tumani",
                "Forish tumani", "G‘allaorol tumani", "Sharof Rashidov tumani", "Mirzacho‘l tumani",
                "Paxtakor tumani", "Yangiobod tumani", "Zafarobod tumani", "Zarbdor tumani",
                "Zomin tumani"
            )
            "Samarqand viloyati" -> listOf(
                "Samarqand shahri", "Kattaqo‘rg‘on shahri", "Bulung‘ur tumani", "Ishtixon tumani",
                "Jomboy tumani", "Kattaqo‘rg‘on tumani", "Narpay tumani", "Nurobod tumani",
                "Oqdaryo tumani", "Paxtachi tumani", "Payariq tumani", "Pastdarg‘om tumani",
                "Qo‘shrabot tumani", "Toyloq tumani", "Urgut tumani"
            )
            "Navoiy viloyati" -> listOf(
                "Navoiy shahri", "Zarafshon shahri", "G‘ozg‘on shahri", "Karmana tumani",
                "Konimex tumani", "Navbahor tumani", "Nurota tumani", "Qiziltepa tumani",
                "Tomdi tumani", "Uchquduq tumani", "Xatirchi tumani"
            )
            "Buxoro viloyati" -> listOf(
                "Buxoro shahri", "Kogon shahri", "Olot tumani", "Buxoro tumani",
                "G‘ijduvon tumani", "Jondor tumani", "Kogon tumani", "Qorako‘l tumani",
                "Qorovulbozor tumani", "Peshku tumani", "Romitan tumani", "Shofirkon tumani",
                "Vobkent tumani"
            )
            "Qashqadaryo viloyati" -> listOf(
                "Qarshi shahri", "Shahrisabz shahri", "G‘uzor tumani", "Dehqonobod tumani",
                "Kasbi tumani", "Kitob tumani", "Koson tumani", "Mirishkor tumani",
                "Muborak tumani", "Nishon tumani", "Chiroqchi tumani", "Qamashi tumani",
                "Qarshi tumani", "Yakkabog‘ tumani"
            )
            "Surxondaryo viloyati" -> listOf(
                "Termiz shahri", "Angor tumani", "Boysun tumani", "Denov tumani",
                "Jarqo‘rg‘on tumani", "Qiziriq tumani", "Qumqo‘rg‘on tumani", "Muzrabot tumani",
                "Oltinsoy tumani", "Sariosiyo tumani", "Sherobod tumani", "Sho‘rchi tumani",
                "Termiz tumani", "Uzun tumani"
            )
            "Farg‘ona viloyati" -> listOf(
                "Farg‘ona shahri", "Qo‘qon shahri", "Marg‘ilon shahri", "Quvasoy shahri",
                "Bag‘dod tumani", "Beshariq tumani", "Buvayda tumani", "Dang‘ara tumani",
                "Furqat tumani", "Oltiariq tumani", "O‘zbekiston tumani", "Qo‘shtepa tumani",
                "Rishton tumani", "So‘x tumani", "Toshloq tumani", "Uchko‘prik tumani",
                "Yozyovon tumani"
            )
            "Andijon viloyati" -> listOf(
                "Andijon shahri", "Asaka shahri", "Xonobod shahri", "Andijon tumani",
                "Asaka tumani", "Baliqchi tumani", "Bo‘ston tumani", "Buloqboshi tumani",
                "Izboskan tumani", "Jalaquduq tumani", "Marhamat tumani", "Oltinko‘l tumani",
                "Paxtaobod tumani", "Qo‘rg‘ontepa tumani", "Shahrixon tumani", "Ulug‘nor tumani",
                "Xo‘jaobod tumani"
            )
            "Namangan viloyati" -> listOf(
                "Namangan shahri", "Chortoq tumani", "Chust tumani", "Kosonsoy tumani",
                "Mingbuloq tumani", "Namangan tumani", "Norin tumani", "Pop tumani",
                "To‘raqo‘rg‘on tumani", "Uchqo‘rg‘on tumani", "Uychi tumani", "Yangiqo‘rg‘on tumani"
            )
            else -> listOf("Tuman/shahar ro‘yxati mavjud emas")
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(when { region == null -> "VILOYAT / SHAHAR"; district == null -> "TUMANNI TANLANG"; else -> "STANSIYA / VOKZAL" }, color = p.accentPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
            TextButton(onClick = onLogout) { Text("Chiqish", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold) }
        }
        Text("Salom, $username!", color = p.textSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        when {
            region == null -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RailData.regions) { r -> ChoiceCard(r.name) { region = r.name } }
            }
            district == null -> {
                TextButton(onClick = { region = null }) { Text("← $region") }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(districts) { d -> ChoiceCard(d) { district = d } }
                }
            }
            else -> {
                TextButton(onClick = { district = null }) { Text("← $region • $district") }
                OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Stansiya yoki vokzalni qidiring") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                val lists = RailData.stationsFor(region!!)
                val allStations = (lists.first + lists.second).distinct()
                val districtStationMap = when (region) {
                    "Xorazm viloyati" -> mapOf(
                        "Urganch shahri" to listOf("Urganch"),
                        "Urganch tumani" to listOf("Urganch"),
                        "Hazorasp tumani" to listOf("Hazorasp"),
                        "Tuproqqal'a tumani" to listOf("Hazorasp", "Pitnak"),
                        "Pitnak shahri" to listOf("Pitnak"),
                        "Xonqa tumani" to listOf("Xonqa"),
                        "Shovot tumani" to listOf("Shovot"),
                        "Bog‘ot tumani" to listOf("Bogʻot", "Bog‘ot"),
                        "Xiva tumani" to listOf("Xiva"),
                        "Gurlan tumani" to listOf("Gurlan"),
                        "Qo‘shko‘pir tumani" to listOf("Qo‘shko‘pir"),
                        "Yangiariq tumani" to listOf("Yangiariq"),
                        "Yangibozor tumani" to listOf("Yangibozor")
                    )
                    "Qoraqalpog‘iston Respublikasi" -> mapOf(
                        "Nukus shahri" to listOf("Nukus"),
                        "Nukus tumani" to listOf("Nukus"),
                        "Qo‘ng‘irot tumani" to listOf("Qoʻngʻirot", "Borsakelmas", "Abadan"),
                        "Taxiatosh shahri" to listOf("Taxiatosh pristani", "Nukus"),
                        "Xo‘jayli tumani" to listOf("Xoʻjayli"),
                        "Beruniy tumani" to listOf("Beruniy"),
                        "To‘rtko‘l tumani" to listOf("Toʻrtkoʻl"),
                        "Chimboy tumani" to listOf("Chimboy"),
                        "Qanliko‘l tumani" to listOf("Qonlikoʻl"),
                        "Shumanay tumani" to listOf("Shumanay"),
                        "Ellikqal'a tumani" to listOf("Ellikqalʼa")
                    )
                    else -> {
                        val districtBase = district
                            .replace(" tumani", "")
                            .replace(" shahri", "")
                            .replace("tumani", "")
                            .replace("shahri", "")
                            .trim()
                        val exactMatches = allStations.filter { normalizeName(it) == normalizeName(districtBase) }
                        if (exactMatches.isNotEmpty()) mapOf(district to exactMatches) else emptyMap()
                    }
                }
                val stations = districtStationMap[district]?.let { mapped ->
                    allStations.filter { stationName -> mapped.any { normalizeName(it) == normalizeName(stationName) } }
                } ?: allStations
                val passengerVokzals = when (region) {
                    "Toshkent shahri" -> listOf("Toshkent-yoʻlovchi", "Toshkent-Janubiy")
                    "Samarqand viloyati" -> listOf("Kattaqoʻrgʻon-yoʻlovchi")
                    "Buxoro viloyati" -> emptyList()
                    "Xorazm viloyati" -> listOf("Xiva")
                    else -> emptyList()
                }.filter { name ->
                    stations.any { normalizeName(it) == normalizeName(name) } &&
                        (query.isBlank() || normalizeName(name).contains(normalizeName(query)))
                }
                val stationChoices = stations.filter { st ->
                    passengerVokzals.none { normalizeName(it) == normalizeName(st) } &&
                        (query.isBlank() || normalizeName(st).contains(normalizeName(query)))
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (passengerVokzals.isNotEmpty()) {
                        item { Text("YO‘LOVCHI VOKZALLARI", color = p.accentPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) }
                        items(passengerVokzals) { st ->
                            ChoiceCard("🚉 Vokzal: $st") { onDone(region!!, district!!, st) }
                        }
                    }
                    item { Text("TEMIR YO‘L STANSIYALARI", color = p.accentPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) }
                    items(stationChoices) { st ->
                        ChoiceCard("🚦 Stansiya: $st") { onDone(region!!, district!!, st) }
                    }
                    item { ChoiceCard("Ro‘yxatda yo‘q — o‘zim kiritaman") { onDone(region!!, district!!, "Boshqa: $district") } }
                }
            }
        }
    }
}

// 2) To'lov oynasi: birinchi 7 kun bepul sinov
// ---------------------------------------------------------------------------

@Composable
fun TrialOfferScreen(
    settings: PaymentSettings,
    station: String,
    onStart: () -> Unit,
    onLogout: () -> Unit
) {
    val p = LocalGlassPalette.current
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        TextButton(onClick = onLogout, modifier = Modifier.align(Alignment.TopEnd)) {
            Text("Chiqish", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = p.glassCardBg),
            border = BorderStroke(1.5.dp, p.glassBorder),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(0.94f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = p.accentPrimary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("TO'LOV", color = p.accentPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Spacer(Modifier.height(4.dp))
                Text(station, color = p.textSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                Text(
                    "Birinchi ${settings.trialDays} kun — BEPUL sinov",
                    color = p.textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Sinov muddati tugagach ilovadan foydalanishni davom ettirish uchun " +
                        "${settings.periodDays} kunga ${formatMoney(settings.price)} so'm to'lanadi.",
                    color = p.textSecondary,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(22.dp))
                GoldButton(
                    text = "BEPUL SINOVNI BOSHLASH",
                    icon = Icons.Default.CardGiftcard,
                    onClick = onStart
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3) Sinov tugadi: to'lov, karta, chekni yuborish
// ---------------------------------------------------------------------------

@Composable
fun PaymentGateScreen(
    settings: PaymentSettings,
    myPayment: PaymentRequest?,
    onSendReceipt: suspend (Uri) -> String?,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    val p = LocalGlassPalette.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var showCard by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            busy = true
            msg = ""
            scope.launch {
                val err = onSendReceipt(uri)
                busy = false
                if (err != null) msg = err
            }
        }
    }

    val pending = myPayment?.status == "pending"
    val rejected = myPayment?.status == "rejected"
    val cardReady = settings.cardNumber.isNotBlank()

    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Yangilash", tint = p.accentPrimary)
            }
            TextButton(onClick = onLogout) {
                Text("Chiqish", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = p.glassCardBg),
            border = BorderStroke(1.5.dp, p.glassBorder),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(0.94f).padding(top = 40.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Payment,
                    contentDescription = null,
                    tint = p.accentPrimary,
                    modifier = Modifier.size(50.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "SINOV MUDDATI TUGADI",
                    color = p.accentPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Davom ettirish uchun to'lov qiling:",
                    color = p.textSecondary,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${formatMoney(settings.price)} so'm",
                    color = p.textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp
                )
                Text("${settings.periodDays} kunga", color = p.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))

                if (pending) {
                    StatusBox("Chek yuborildi. Admin tekshirmoqda — tasdiqlangach ilova o'zi ochiladi.", Color(0xFFF59E0B))
                    Spacer(Modifier.height(12.dp))
                } else if (rejected) {
                    StatusBox("Chek rad etildi. To'g'ri chekni qayta yuboring.", Color(0xFFEF4444))
                    Spacer(Modifier.height(12.dp))
                }

                GoldButton(
                    text = "TO'LOV QILISH",
                    icon = Icons.Default.CreditCard,
                    onClick = { showCard = !showCard }
                )

                AnimatedVisibility(visible = showCard) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(14.dp))
                        if (!cardReady) {
                            StatusBox(
                                "Admin hali karta qo'shmagan. Birozdan so'ng yangilash tugmasini bosing.",
                                Color(0xFFF59E0B)
                            )
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = p.glassSurfaceBg),
                                border = BorderStroke(1.dp, p.glassBorder),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Karta raqami", color = p.textSecondary, fontSize = 12.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            settings.cardNumber,
                                            color = p.textPrimary,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 20.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = {
                                            clipboard.setText(AnnotatedString(settings.cardNumber.replace(" ", "")))
                                        }) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "Nusxa olish",
                                                tint = p.accentPrimary
                                            )
                                        }
                                    }
                                    if (settings.cardOwner.isNotBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        Text("Karta egasi", color = p.textSecondary, fontSize = 12.sp)
                                        Text(
                                            settings.cardOwner,
                                            color = p.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "1) Yuqoridagi kartaga ${formatMoney(settings.price)} so'm o'tkazing.\n" +
                                    "2) Pastdagi tugma orqali chekni (skrinshot) yuboring.\n" +
                                    "3) Admin tasdiqlagach ilova ochiladi.",
                                color = p.textSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(14.dp))
                            GoldButton(
                                text = if (busy) "YUBORILMOQDA..." else "CHEKNI QO'YING",
                                icon = Icons.Default.Receipt,
                                enabled = !busy && !pending,
                                onClick = { picker.launch("image/*") }
                            )
                        }
                    }
                }

                if (msg.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(msg, color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StatusBox(text: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(12.dp)
    ) {
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Chek rasmini kichraytirib (eng uzun tomoni ~1600 px) JPEG qilib base64 ga o'giradi.
 * Fon oqimida chaqiriladi. Xato bo'lsa null qaytaradi.
 */
fun compressReceipt(context: Context, uri: Uri): String? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxSide / sample > 1600) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 70, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}
