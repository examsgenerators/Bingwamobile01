package com.bingwa.mobile

import android.content.Context

class TokenManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("TokenStore", Context.MODE_PRIVATE)
    
    fun getBalance(): Int = prefs.getInt("balance", 0)
    
    fun addTokens(amount: Int) {
        prefs.edit().putInt("balance", getBalance() + amount).apply()
    }
    
    fun deductTokens(amount: Int): Boolean {
        val current = getBalance()
        return if (current >= amount) {
            prefs.edit().putInt("balance", current - amount).apply()
            true
        } else false
    }
}
