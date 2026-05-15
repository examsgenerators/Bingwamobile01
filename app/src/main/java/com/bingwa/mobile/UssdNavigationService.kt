package com.bingwa.mobile

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Bundle
import android.util.Log

class UssdNavigationService : AccessibilityService() {
    
    companion object {
        private const val TAG = "UssdNavigation"
        var airtimeBalance = "N/A"
        var balanceCallback: ((String) -> Unit)? = null
    }
    
    // Default navigation steps for data bundle purchase
    private var steps = arrayOf("6", "1")
    private var currentStep = 0
    private var isBalanceCheck = false
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "✅ USSD Navigation Service Connected")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return
        
        val nodeInfo = event.source ?: return
        
        // Read all text from the USSD dialog
        val allText = extractAllText(nodeInfo)
        Log.d(TAG, "USSD Screen: $allText")
        
        // Check if this is an airtime balance response
        if (allText.contains("balance", ignoreCase = true) || 
            allText.contains("airtime", ignoreCase = true) ||
            allText.contains("Ksh", ignoreCase = true)) {
            airtimeBalance = allText
            balanceCallback?.invoke(allText)
            isBalanceCheck = false
            performGlobalAction(GLOBAL_ACTION_BACK)
            return
        }
        
        // Navigate USSD menus
        val inputs = nodeInfo.findAccessibilityNodeInfosByViewId("com.android.phone:id/input_field")
        
        if (inputs != null && inputs.isNotEmpty() && currentStep < steps.size) {
            val inputField = inputs[0]
            val args = Bundle()
            args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                steps[currentStep]
            )
            inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            
            // Click Send button
            val buttons = nodeInfo.findAccessibilityNodeInfosByText("Send")
            for (btn in buttons) {
                btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            
            currentStep++
        } else if (currentStep >= steps.size) {
            currentStep = 0 // Reset for next session
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }
    
    private fun extractAllText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        if (node.text != null) sb.append(node.text).append(" ")
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            sb.append(extractAllText(child))
            child.recycle()
        }
        return sb.toString().trim()
    }
    
    fun setNavigationSteps(newSteps: Array<String>) {
        steps = newSteps
        currentStep = 0
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "USSD Service Interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "USSD Service Destroyed")
    }
}
