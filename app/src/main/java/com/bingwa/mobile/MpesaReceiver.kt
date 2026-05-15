package com.bingwa.mobile

import android.content.*
import android.telephony.SmsMessage
import android.util.Log

class MpesaReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "MpesaReceiver"
        private val TOKEN_NAMES = listOf("victor ngetich", "victor kiplangat ngetich")
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        // Check if automation is enabled
        val appPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        if (!appPrefs.getBoolean("automation_enabled", true)) {
            Log.d(TAG, "Automation disabled - ignoring SMS")
            return
        }
        
        val pdus = intent.extras?.get("pdus") as? Array<*> ?: return
        
        for (pdu in pdus) {
            val sms = SmsMessage.createFromPdu(pdu as ByteArray)
            val body = sms.messageBody ?: continue
            val sender = sms.originatingAddress ?: ""
            
            // Only process M-PESA messages
            if (!sender.equals("MPESA", ignoreCase = true)) continue
            
            Log.d(TAG, "SMS: $body")
            
            val lowerBody = body.lowercase()
            
            // Check if this is a TOKEN PURCHASE (sent TO Victor Ngetich)
            val isTokenPurchase = TOKEN_NAMES.any { lowerBody.contains(it) }
            
            if (isTokenPurchase) {
                handleTokenPurchase(context, body)
            } else if (lowerBody.contains("received")) {
                // This is a DATA SELLING request (received FROM client)
                handleDataSelling(context, body)
            }
        }
    }
    
    private fun handleTokenPurchase(context: Context, body: String) {
        val tokens = context.getSharedPreferences("TokenStore", Context.MODE_PRIVATE)
        val currentBalance = tokens.getInt("balance", 0)
        val amount = extractAmount(body)
        
        val tokensToAdd = when {
            amount >= 100 -> 1000
            amount >= 50 -> 500
            amount >= 10 -> 90
            else -> 0
        }
        
        if (tokensToAdd > 0) {
            tokens.edit().putInt("balance", currentBalance + tokensToAdd).apply()
            Log.d(TAG, "✅ Tokens added: $tokensToAdd (KSh $amount)")
            
            // Show notification
            showNotification(context, "Tokens Added", "+$tokensToAdd tokens (KSh $amount)")
        }
    }
    
    private fun handleDataSelling(context: Context, body: String) {
        val tokens = context.getSharedPreferences("TokenStore", Context.MODE_PRIVATE)
        val currentBalance = tokens.getInt("balance", 0)
        val amount = extractAmount(body)
        val phoneNumber = extractPhoneNumber(body)
        
        // Get data offers from settings
        val prefs = context.getSharedPreferences("DataOffers", Context.MODE_PRIVATE)
        val offersJson = prefs.getString("offers", "[]") ?: "[]"
        val gson = com.google.gson.Gson()
        val offers: List<DataOffer> = try {
            gson.fromJson(offersJson, Array<DataOffer>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
        
        // Find matching offer
        val offer = offers.find { it.price == amount }
        
        if (offer != null && currentBalance >= offer.tokenCost) {
            // Deduct tokens
            tokens.edit().putInt("balance", currentBalance - offer.tokenCost).apply()
            
            // Execute USSD
            val service = Intent(context, AutomationService::class.java)
            service.putExtra("mode", offer.executionMode)
            service.putExtra("code", offer.ussdCode)
            service.putExtra("phoneNumber", phoneNumber)
            context.startService(service)
            
            Log.d(TAG, "🚀 Executing: ${offer.name} for $phoneNumber")
            showNotification(context, "Executing", "${offer.name} for $phoneNumber")
        } else if (offer != null && currentBalance < offer.tokenCost) {
            Log.d(TAG, "❌ Insufficient tokens: have $currentBalance, need ${offer.tokenCost}")
            showNotification(context, "Insufficient Tokens", "Need ${offer.tokenCost} tokens, have $currentBalance")
        }
    }
    
    private fun extractAmount(body: String): Int {
        val regex = Regex("Ksh\\s*(\\d+)", RegexOption.IGNORE_CASE)
        return regex.find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }
    
    private fun extractPhoneNumber(body: String): String {
        val regex = Regex("07\\d{8}")
        return regex.find(body)?.value ?: ""
    }
    
    private fun showNotification(context: Context, title: String, message: String) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    "bingwa_transactions",
                    "Bingwa Transactions",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                )
                manager.createNotificationChannel(channel)
            }
            val notification = androidx.core.app.NotificationCompat.Builder(context, "bingwa_transactions")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            manager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.e(TAG, "Notification failed: ${e.message}")
        }
    }
}