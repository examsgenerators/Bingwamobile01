package com.bingwa.mobile

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.util.Log

class BalanceChecker : Service() {
    
    companion object {
        private const val TAG = "BalanceChecker"
        var currentBalance = "Checking..."
        var balanceCallback: ((String) -> Unit)? = null
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    
    private val balanceRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                checkBalance()
                handler.postDelayed(this, 4000) // Every 4 seconds
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        isRunning = true
        handler.post(balanceRunnable)
        Log.d(TAG, "✅ Balance Checker Started (every 4s)")
    }
    
    private fun checkBalance() {
        // Check if automation is enabled
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("automation_enabled", true)) {
            Log.d(TAG, "Automation disabled - stopping balance check")
            stopSelf()
            return
        }
        
        try {
            val simId = prefs.getInt("selected_sim_id", -1)
            val uri = Uri.parse("tel:*144%23")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && simId != -1) {
                val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                val phoneAccountHandle = subscriptionManager?.getPhoneAccountHandleForSubscriptionId(simId)
                
                if (telecomManager != null && phoneAccountHandle != null) {
                    val extras = Bundle()
                    extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
                    telecomManager.placeCall(uri, extras)
                    Log.d(TAG, "📞 Balance check with SIM $simId")
                } else {
                    // Fallback to normal call
                    val intent = Intent(Intent.ACTION_CALL, uri)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            } else {
                // No SIM selection or old API, use normal call
                val intent = Intent(Intent.ACTION_CALL, uri)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                Log.d(TAG, "📞 Balance check (default SIM)")
            }
            
            // Set callback for UssdNavigationService to update balance
            UssdNavigationService.balanceCallback = { balance ->
                currentBalance = balance
                balanceCallback?.invoke(balance)
                Log.d(TAG, "Balance updated: $balance")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking balance: ${e.message}")
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(balanceRunnable)
        Log.d(TAG, "Balance Checker Stopped")
    }
}