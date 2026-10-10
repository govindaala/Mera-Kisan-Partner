// app/src/main/java/in/merakisan/app/core/notifications/NotificationHelper.kt
package in.merakisan.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import in.merakisan.app.ui.main.MainActivity

/**
 * MERA KISAN Central Notification Manager
 * Android 8.0+ (Oreo) चैनल्स और Android 13+ हेड्स-अप अलर्ट का प्रबंधन
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ORDERS = "channel_orders"
        const val CHANNEL_OFFERS = "channel_offers"
        const val CHANNEL_MARKETPLACE = "channel_marketplace"
        const val CHANNEL_SYSTEM = "channel_system"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val orderChannel = NotificationChannel(
                CHANNEL_ORDERS,
                "ऑर्डर्स व डिलीवरी (Orders)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "नए ऑर्डर, डिलीवरी और भुगतान अपडेट"
                enableVibration(true)
            }

            val offerChannel = NotificationChannel(
                CHANNEL_OFFERS,
                "भाव प्रस्ताव व नेगोशिएशन (Offers)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "किसान और खरीदार के आपसी भाव प्रस्ताव व काउंटर ऑफ़र"
                enableVibration(true)
            }

            val marketChannel = NotificationChannel(
                CHANNEL_MARKETPLACE,
                "मांग बोर्ड व फसल अलर्ट (Marketplace)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "'मुझे चाहिए' बोर्ड की नई मांगें और ताज़ा फसल अलर्ट"
            }

            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM,
                "महत्वपूर्ण सूचनाएं (System)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "खाता सुरक्षा, नियम और ऐप अपडेट"
            }

            notificationManager.createNotificationChannels(
                listOf(orderChannel, offerChannel, marketChannel, systemChannel)
            )
        }
    }

    fun showNotification(
        channelId: String,
        title: String,
        message: String,
        targetId: String? = null,
        notificationType: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("TARGET_ID", targetId)
            putExtra("NOTIFICATION_TYPE", notificationType)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        notificationManager.notify(notificationId, builder.build())
    }
}
