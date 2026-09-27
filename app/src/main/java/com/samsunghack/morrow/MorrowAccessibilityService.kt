package com.samsunghack.morrow

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.CopyOnWriteArrayList

class MorrowAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "MorrowAccessibility"
        private const val MIN_DELAY_MS = 40L

        @Volatile var instance: MorrowAccessibilityService? = null
            private set

        fun startTeaching(): Boolean = instance?.startRecording() == true
        fun stopTeaching(): List<RecordedAction> = instance?.finishRecording().orEmpty()
    }

    private val recordedActions = CopyOnWriteArrayList<RecordedAction>()
    private var recording = false
    private var lastEventAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val source = event.source ?: return
        if (recording) capture(event, source)
        source.recycle()
    }

    private fun capture(event: AccessibilityEvent, source: AccessibilityNodeInfo) {
        if (isSensitive(source)) return

        val now = SystemClock.uptimeMillis()
        val delay = if (lastEventAt == 0L) 0L else (now - lastEventAt).coerceAtLeast(MIN_DELAY_MS)
        lastEventAt = now

        val packageName = source.packageName?.toString()
        val className = source.className?.toString()
        val viewId = source.viewIdResourceName
        val description = source.contentDescription?.toString()
        val bounds = android.graphics.Rect().also { source.getBoundsInScreen(it) }

        val common = { type: ActionType, text: String? ->
            RecordedAction(type, text, className, viewId, packageName, description,
                bounds.left, bounds.top, bounds.right, bounds.bottom, delay)
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED ->
                recordedActions.add(common(ActionType.CLICK, null))

            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val text = event.text?.joinToString("")?.takeIf { it.isNotBlank() }
                if (text != null) recordedActions.add(common(ActionType.TYPE_TEXT, text))
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED ->
                recordedActions.add(common(ActionType.SCROLL, null))
        }
    }

    private fun isSensitive(node: AccessibilityNodeInfo): Boolean {
        if (node.isPassword) return true

        val haystack = buildString {
            append(node.hintText ?: "")
            append(" ")
            append(node.text ?: "")
            append(" ")
            append(node.contentDescription ?: "")
            append(" ")
            append(node.viewIdResourceName ?: "")
        }.lowercase()

        return listOf(
            "password", "passwd", "passcode", "otp", "one-time password",
            "verification code", "cvv", "card number", "credit card",
            "debit card", "pin"
        ).any(haystack::contains)
    }

    private fun startRecording(): Boolean {
        recordedActions.clear()
        lastEventAt = 0L
        recording = true
        Log.i(TAG, "Teaching started")
        return true
    }

    private fun finishRecording(): List<RecordedAction> {
        recording = false
        val result = recordedActions.toList()
        recordedActions.clear()
        lastEventAt = 0L
        Log.i(TAG, "Teaching stopped: " + result.size + " actions")
        return result
    }

    override fun onInterrupt() { recording = false }

    override fun onDestroy() {
        instance = null
        recording = false
        super.onDestroy()
    }
}
