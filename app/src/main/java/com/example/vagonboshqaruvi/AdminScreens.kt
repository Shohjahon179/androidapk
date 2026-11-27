package com.example.vagonboshqaruvi

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GreenOk = Color(0xFF22C55E)
private val RedNo = Color(0xFFEF4444)
private val AmberWait = Color(0xFFF59E0B)

@Composable
fun AdminPanelScreen(
    users: List<User>,
    payments: List<PaymentRequest>,
    settings: PaymentSettings,
    nowMs: Long,
    isNightGlass: Boolean,
    isCloudOnline: Boolean,
    onToggleTheme: () -> Unit,
    onSyncNow: () -> Unit,
    onGrant: (User) -> Unit,
    onDeleteUser: (User) -> Unit,
    onApprove: (PaymentRequest) -> Unit,
    onReject: (PaymentRequest) -> Unit,
    onSaveSettings: suspend (PaymentSettings) -> String?,
    loadReceipt: suspend (String) -> String?,
    onLogout: () -> Unit
) {
    val p = LocalGlassPalette.current
    var tab by remember { mutableStateOf(0) }
    val pendingCount = payments.count { it.status == "pending" }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ADMIN PANEL", color = p.accentPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CloudStatusBadge(isOnline = isCloudOnline, onSyncClick = onSyncNow)
                GlassThemeToggleChip(isNightGlass = isNightGlass, onToggleTheme = onToggleTheme)
                TextButton(onClick = onLogout) {
                    Text("Chiqish", color = RedNo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        TabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent,
            contentColor = p.accentPrimary
        ) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text("Foydalanuvchilar", fontSize = 11.sp, maxLines = 1) }
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = {
                    Text(
                        if (pendingCount > 0) "To'lovlar ($pendingCount)" else "To'lovlar",
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            )
            Tab(
                selected = tab == 2,
                onClick = { tab = 2 },
                text = { Text("Karta", fontSize = 11.sp, maxLines = 1) }
            )
        }
        Spacer(Modifier.height(12.dp))

        when (tab) {
            0 -> AdminUsersTab(users, settings, nowMs, onGrant, onDeleteUser)
            1 -> AdminPaymentsTab(payments, onApprove, onReject, loadReceipt)
            else -> AdminCardTab(settings, onSaveSettings)
        }
    }
}

@Composable
private fun AdminUsersTab(
    users: List<User>,
    settings: PaymentSettings,
    nowMs: Long,
    onGrant: (User) -> Unit,
    onDeleteUser: (User) -> Unit
) {
    val p = LocalGlassPalette.current
    var toDelete by remember { mutableStateOf<User?>(null) }
    val shown = users.filter { it.role != "Admin" }

    Text(
        "Ro'yxatdan o'tganlar: ${shown.size} ta",
        color = p.textSecondary,
        fontSize = 12.sp
    )
    Spacer(Modifier.height(8.dp))

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(shown, key = { it.id }) { u ->
            val access = Subscription.access(u, nowMs, settings)
            val (statusText, statusColor) = when (access.state) {
                AccessState.NOT_STARTED -> Pair("Sinov hali boshlanmagan", p.textSecondary)
                AccessState.TRIAL -> Pair("Bepul sinov: ${access.daysLeft} kun qoldi", AmberWait)
                AccessState.PAID -> Pair("To'langan: ${access.daysLeft} kun qoldi", GreenOk)
                AccessState.EXPIRED -> Pair("YOPILGAN — muddati tugagan", RedNo)
                AccessState.ADMIN -> Pair("Admin", p.accentPrimary)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = p.glassCardBg),
                border = BorderStroke(1.2.dp, p.glassBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Login: ${u.username}", color = p.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (u.phoneNumber.isNotBlank()) {
                        Text("Telefon: ${u.phoneNumber}", color = p.textSecondary, fontSize = 13.sp)
                    }
                    Text("Parol: ${u.password}", color = p.accentPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text("Lavozim: ${u.role}", color = p.accentSecondary, fontSize = 13.sp)
                    val place = if (u.region.isBlank()) "tanlanmagan" else "${u.region} • ${u.district} • ${u.station}"
                    Text("Hudud: $place", color = p.textSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onGrant(u) },
                            border = BorderStroke(1.dp, GreenOk),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = GreenOk, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ochish +${settings.periodDays} kun", color = GreenOk, fontSize = 11.sp, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { toDelete = u },
                            border = BorderStroke(1.dp, RedNo),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = RedNo, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("O'chirish", color = RedNo, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    val target = toDelete
    if (target != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Akkauntni o'chirish") },
            text = { Text("\"${target.username}\" akkaunti butunlay o'chiriladi. Davom etasizmi?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteUser(target)
                    toDelete = null
                }) { Text("O'chirish", color = RedNo, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Bekor qilish") }
            }
        )
    }
}

@Composable
private fun AdminPaymentsTab(
    payments: List<PaymentRequest>,
    onApprove: (PaymentRequest) -> Unit,
    onReject: (PaymentRequest) -> Unit,
    loadReceipt: suspend (String) -> String?
) {
    val p = LocalGlassPalette.current
    var receiptFor by remember { mutableStateOf<PaymentRequest?>(null) }
    val sorted = payments.sortedWith(
        compareBy<PaymentRequest> { if (it.status == "pending") 0 else 1 }.thenByDescending { it.createdAt }
    )
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    if (sorted.isEmpty()) {
        Text("Hozircha to'lov cheklari yo'q.", color = p.textSecondary, fontSize = 14.sp)
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(sorted, key = { it.id }) { pay ->
            val (label, color) = when (pay.status) {
                "approved" -> Pair("Tasdiqlangan", GreenOk)
                "rejected" -> Pair("Rad etilgan", RedNo)
                else -> Pair("Kutilmoqda", AmberWait)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = p.glassCardBg),
                border = BorderStroke(1.2.dp, if (pay.status == "pending") AmberWait else p.glassBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(pay.username, color = p.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(label, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                    if (pay.phoneNumber.isNotBlank()) {
                        Text("Telefon: ${pay.phoneNumber}", color = p.textSecondary, fontSize = 13.sp)
                    }
                    Text("${pay.region} • ${pay.station}", color = p.textSecondary, fontSize = 13.sp)
                    Text(
                        "${formatMoney(pay.amount)} so'm  •  ${fmt.format(Date(pay.createdAt))}",
                        color = p.accentPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { receiptFor = pay },
                        border = BorderStroke(1.dp, p.accentPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = p.accentPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Chekni ko'rish", color = p.accentPrimary, fontSize = 12.sp)
                    }
                    if (pay.status == "pending") {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onApprove(pay) },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenOk),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Tasdiqlash", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                            }
                            Button(
                                onClick = { onReject(pay) },
                                colors = ButtonDefaults.buttonColors(containerColor = RedNo),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Rad etish", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    val shown = receiptFor
    if (shown != null) {
        ReceiptDialog(payment = shown, loadReceipt = loadReceipt, onDismiss = { receiptFor = null })
    }
}

@Composable
private fun ReceiptDialog(
    payment: PaymentRequest,
    loadReceipt: suspend (String) -> String?,
    onDismiss: () -> Unit
) {
    var loading by remember(payment.id) { mutableStateOf(true) }
    var bitmap by remember(payment.id) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(payment.id) {
        loading = true
        val b64 = loadReceipt(payment.id)
        bitmap = if (b64 == null) null else withContext(Dispatchers.Default) {
            try {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chek — ${payment.username}") },
        text = {
            val img = bitmap
            when {
                loading -> Text("Yuklanmoqda...")
                img == null -> Text("Chekni yuklab bo'lmadi. Internetni tekshirib qayta oching.")
                else -> Image(
                    bitmap = img,
                    contentDescription = "Chek",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Yopish") } }
    )
}

@Composable
private fun AdminCardTab(
    settings: PaymentSettings,
    onSaveSettings: suspend (PaymentSettings) -> String?
) {
    val p = LocalGlassPalette.current
    val scope = rememberCoroutineScope()
    var cardNumber by remember(settings) { mutableStateOf(settings.cardNumber) }
    var cardOwner by remember(settings) { mutableStateOf(settings.cardOwner) }
    var price by remember(settings) { mutableStateOf(settings.price.toString()) }
    var period by remember(settings) { mutableStateOf(settings.periodDays.toString()) }
    var trial by remember(settings) { mutableStateOf(settings.trialDays.toString()) }
    var info by remember { mutableStateOf("") }
    var infoOk by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text(
            "Foydalanuvchilar to'lovni shu kartaga qiladi. Karta raqami to'lov oynasida ko'rinadi.",
            color = p.textSecondary,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = cardNumber,
            onValueChange = { cardNumber = it; info = "" },
            label = { Text("Karta raqami", color = p.textSecondary) },
            leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = p.accentPrimary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = glassTextFieldColors()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = cardOwner,
            onValueChange = { cardOwner = it; info = "" },
            label = { Text("Karta egasi (ism familiya)", color = p.textSecondary) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = p.accentPrimary) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = glassTextFieldColors()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = price,
            onValueChange = { price = it.filter { c -> c.isDigit() }.take(9); info = "" },
            label = { Text("Narx (so'm)", color = p.textSecondary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = glassTextFieldColors()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = period,
            onValueChange = { period = it.filter { c -> c.isDigit() }.take(4); info = "" },
            label = { Text("Bitta to'lov necha kunga ochadi", color = p.textSecondary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = glassTextFieldColors()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = trial,
            onValueChange = { trial = it.filter { c -> c.isDigit() }.take(3); info = "" },
            label = { Text("Bepul sinov muddati (kun)", color = p.textSecondary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(), colors = glassTextFieldColors()
        )
        Spacer(Modifier.height(14.dp))
        GoldButton(
            text = if (busy) "SAQLANMOQDA..." else "SAQLASH",
            icon = Icons.Default.CheckCircle,
            enabled = !busy,
            onClick = {
                val priceInt = price.toIntOrNull() ?: 0
                val periodInt = period.toIntOrNull() ?: 0
                val trialInt = trial.toIntOrNull() ?: 0
                if (priceInt <= 0 || periodInt <= 0 || trialInt <= 0) {
                    infoOk = false
                    info = "Narx va kun soni 0 dan katta bo'lishi kerak."
                } else {
                    busy = true
                    scope.launch {
                        val err = onSaveSettings(
                            settings.copy(
                                cardNumber = cardNumber.trim(),
                                cardOwner = cardOwner.trim(),
                                price = priceInt,
                                periodDays = periodInt,
                                trialDays = trialInt
                            )
                        )
                        busy = false
                        infoOk = err == null
                        info = err ?: "Saqlandi."
                    }
                }
            }
        )
        if (info.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                info,
                color = if (infoOk) GreenOk else RedNo,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}
