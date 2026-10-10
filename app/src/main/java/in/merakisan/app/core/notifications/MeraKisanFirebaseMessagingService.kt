// app/src/main/java/in/merakisan/app/core/notifications/MeraKisanFirebaseMessagingService.kt
package in.merakisan.app.core.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import in.merakisan.app.core.config.FeatureManager
import in.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * MERA KISAN Background Push Listener
 * बैकग्राउंड और फ़ोरग्राउंड में आने वाले FCM पेलोड्स को प्रोसेस करने वाला इंजन
 */
class MeraKisanFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // नए FCM टोकन को सत्र में सुरक्षित रखना
        SessionManager.saveFcmToken(applicationContext, token)
        syncTokenWithBackend(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // रिमोट फ़ीचर गार्ड: यदि एडमिन ने नोटिफिकेशन बंद कर रखे हैं तो कोई अलर्ट न दिखाएं
        val isPushEnabled = FeatureManager.isEnabled(applicationContext, "notifications")
        if (!isPushEnabled) return

        val helper = NotificationHelper(applicationContext)

        // 1. डेटा पेलोड से कस्टम एग्रो-इवेंट्स पढ़ना
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val type = data["type"] ?: "SYSTEM"
            val title = data["title"] ?: remoteMessage.notification?.title ?: "MERA KISAN"
            val message = data["message"] ?: remoteMessage.notification?.body ?: "नई सूचना प्राप्त हुई"
            val targetId = data["target_id"]

            val channelId = when (type) {
                "ORDER_PLACED", "ORDER_ACCEPTED", "ORDER_DELIVERED" -> NotificationHelper.CHANNEL_ORDERS
                "OFFER_RECEIVED", "OFFER_ACCEPTED", "OFFER_COUNTERED" -> NotificationHelper.CHANNEL_OFFERS
                "BUYER_REQUEST_MATCH", "NEW_HARVEST" -> NotificationHelper.CHANNEL_MARKETPLACE
                else -> NotificationHelper.CHANNEL_SYSTEM
            }

            helper.showNotification(
                channelId = channelId,
                title = title,
                message = message,
                targetId = targetId,
                notificationType = type
            )
        } else {
            // 2. सामान्य नोटिफिकेशन फ़ॉलबैक
            remoteMessage.notification?.let { notif ->
                helper.showNotification(
                    channelId = NotificationHelper.CHANNEL_SYSTEM,
                    title = notif.title ?: "MERA KISAN",
                    message = notif.body ?: ""
                )
            }
        }
    }

    private fun syncTokenWithBackend(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            // बैकएंड के /v1/users/fcm-token पर टोकन सुरक्षित रूप से सिंक करने की पृष्ठभूमि प्रक्रिया
        }
    }
}
