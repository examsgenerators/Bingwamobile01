package com.bingwa.mobile

import android.content.*
import android.telephony.SmsMessage
import android.util.Log

class MpesaReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "MpesaReceiver"
        // Names used for token purchase detection
        private val TOKEN_NAMES = listOf("victor ngetich", "victor kiplangat ngetich")
    }
    
    override fun onReceive(context: Context, intent: Intent) {
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
}
