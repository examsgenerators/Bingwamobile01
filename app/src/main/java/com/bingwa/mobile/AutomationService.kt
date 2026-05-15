package com.bingwa.mobile

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.util.Log

class AutomationService : Service() {

    companion object {
        private const val TAG = "AutomationService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ussdCode = intent?.getStringExtra("code") ?: return START_NOT_STICKY
        val mode = intent.getStringExtra("mode") ?: "SIMPLE"
        val phoneNumber = intent.getStringExtra("phoneNumber") ?: ""

        Log.d(TAG, "Mode: $mode, Code: $ussdCode, Phone: $phoneNumber")

        if (mode.equals("SIMPLE", ignoreCase = true)) {
            dial(ussdCode + Uri.encode("#"))
        } else {
            val base = ussdCode.split("*").firstOrNull { it.isNotEmpty() } ?: ussdCode
            dial(base + Uri.encode("#"))
        }
        return START_NOT_STICKY
    }

    private fun dial(fullCode: String) {
        try {
            val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val simId = prefs.getInt("selected_sim_id", -1)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && simId != -1) {
                // Use TelecomManager to place call with specific SIM
                val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                val phoneAccountHandle = subscriptionManager.getPhoneAccountHandleForSubscriptionId(simId)

                if (phoneAccountHandle != null) {
                    val uri = Uri.parse("tel:$fullCode")
                    val extras = Bundle()
                    extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
                    telecomManager.placeCall(uri, extras)
                    Log.d(TAG, "📞 Dialing with SIM $simId: $fullCode")
                } else {
                    // Fallback to normal call
                    val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$fullCode"))
                    callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(callIntent)
                }
            } else {
                // No SIM selection or old API, use normal call
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$fullCode"))
                callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(callIntent)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Permission denied: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}