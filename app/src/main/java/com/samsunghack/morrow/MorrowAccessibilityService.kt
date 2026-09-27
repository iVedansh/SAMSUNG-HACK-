package com.samsunghack.morrow

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class MorrowAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "MorrowAccessibility"

        @Volatile
        var instance: MorrowAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val source = event.source
        Log.d(TAG, "event=" + event.eventType +
            " package=" + event.packageName +
            " class=" + source?.className +
            " text=" + source?.text)
    }

    override fun onInterrupt() {
        Log.i(TAG, "Accessibility Service interrupted")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
