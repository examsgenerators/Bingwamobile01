package com.bingwa.mobile

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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
        try {
            // Dial *144# for airtime balance
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:*144%23"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            
            // Set callback for UssdNavigationService to update balance
            UssdNavigationService.balanceCallback = { balance ->
                currentBalance = balance
                balanceCallback?.invoke(balance)
                Log.d(TAG, "Balance updated: $balance")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied: ${e.message}")
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
