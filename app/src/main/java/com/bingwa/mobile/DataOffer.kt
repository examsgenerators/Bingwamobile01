package com.bingwa.mobile

data class DataOffer(
    val name: String,        // e.g., "250MB Data Bundle"
    val price: Int,          // e.g., 20 (KSh)
    val tokenCost: Int,      // e.g., 10 (tokens deducted)
    val ussdCode: String,    // e.g., "*180*5*2*1#"
    val executionMode: String // "SIMPLE" or "ADVANCED"
)
