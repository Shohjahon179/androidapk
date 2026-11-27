package com.example.vagonboshqaruvi

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object SubscriptionReminder {
    private const val CHANNEL_ID = "premium_expiry_reminder"
    private const val EXTRA_USERNAME = "username"
    private const val EXTRA_USER_ID = "user_id"

    fun schedule(context: Context, user: User, settings: PaymentSettings) {
        if (user.role == "Admin") return
        val endAt = Subscription.endAt(user, settings)
        if (endAt <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SubscriptionReminderReceiver::class.java).apply {
            putExtra(EXTRA_USERNAME, user.username)
            putExtra(EXTRA_USER_ID, user.id)
        }
        val requestCode = user.id.hashCode()
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Eslatma obuna tugashidan 24 soat oldin. Agar foydalanuvchi shu 24 soat ichida kirsa,
        // eslatma darholga yaqin vaqtga qo'yiladi.
        val triggerAt = maxOf(System.currentTimeMillis() + 5_000L, endAt - Subscription.DAY_MS)
        alarmManager.cancel(pending)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    internal fun showNotification(context: Context, username: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Premium muddati eslatmalari",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Premium yoki sinov muddati tugashidan bir kun oldin eslatadi"
            }
            manager.createNotificationChannel(channel)
        }

        val openApp = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = openApp?.let {
            PendingIntent.getActivity(
                context,
                7401,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Vagon Control — obuna eslatmasi")
            .setContentText("$username, Premium/sinov muddati ertaga tugaydi. Obunani uzaytirishni unutmang.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$username, Premium yoki sinov muddatingiz ertaga tugaydi. Ilovaga kirib obunani uzaytiring."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .apply { if (contentIntent != null) setContentIntent(contentIntent) }
            .build()

        try {
            NotificationManagerCompat.from(context).notify(username.hashCode(), notification)
        } catch (_: SecurityException) {
            // Android 13+ da foydalanuvchi bildirishnomaga ruxsat bermagan bo'lishi mumkin.
        }
    }

    internal const val USERNAME_EXTRA = EXTRA_USERNAME
}

class SubscriptionReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val username = intent.getStringExtra(SubscriptionReminder.USERNAME_EXTRA).orEmpty()
        if (username.isNotBlank()) {
            SubscriptionReminder.showNotification(context, username)
        }
    }
}
