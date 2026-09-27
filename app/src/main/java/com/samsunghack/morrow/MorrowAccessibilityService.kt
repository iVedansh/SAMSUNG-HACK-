package com.samsunghack.morrow

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.CopyOnWriteArrayList

class MorrowAccessibilityService : AccessibilityService() {
    companion object {
        private const val MIN_DELAY_MS = 40L

        @Volatile var instance: MorrowAccessibilityService? = null
            private set

        fun startTeaching(): Boolean = instance?.startRecording() == true
        fun stopTeaching(): List<RecordedAction> = instance?.finishRecording().orEmpty()

        fun runRoutine(routine: Routine, onFinished: (Boolean, String) -> Unit) {
            instance?.executeRoutine(routine, onFinished)
                ?: onFinished(false, "Accessibility Service is not connected.")
        }
    }

    private val recordedActions = CopyOnWriteArrayList<RecordedAction>()
    private var recording = false
    private var lastEventAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
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
        val bounds = android.graphics.Rect().also { source.getBoundsInScreen(it) }

        val type = when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> ActionType.CLICK
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> ActionType.TYPE_TEXT
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> ActionType.SCROLL
            else -> return
        }

        val scrollForward = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            event.scrollDeltaY >= 0
        } else {
            true
        }

        val action = RecordedAction(
            type = type,
            text = event.text?.joinToString("")?.takeIf { it.isNotBlank() },
            className = source.className?.toString(),
            viewId = source.viewIdResourceName,
            packageName = source.packageName?.toString(),
            contentDescription = source.contentDescription?.toString(),
            boundsLeft = bounds.left,
            boundsTop = bounds.top,
            boundsRight = bounds.right,
            boundsBottom = bounds.bottom,
            delayMs = delay,
            scrollForward = scrollForward
        )

        if (type == ActionType.TYPE_TEXT) {
            val previous = recordedActions.lastOrNull()
            if (previous?.type == ActionType.TYPE_TEXT &&
                previous.viewId == action.viewId &&
                previous.packageName == action.packageName
            ) {
                recordedActions[recordedActions.lastIndex] = action.copy(delayMs = previous.delayMs)
                return
            }
        }

        recordedActions.add(action)
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
        return true
    }

    private fun finishRecording(): List<RecordedAction> {
        recording = false
        val result = recordedActions.toList()
        recordedActions.clear()
        lastEventAt = 0L
        return result
    }

    private fun executeRoutine(routine: Routine, onFinished: (Boolean, String) -> Unit) {
        Thread {
            for ((index, action) in routine.actions.withIndex()) {
                if (action.delayMs > 0) SystemClock.sleep(action.delayMs.coerceIn(40L, 5000L))
                if (!perform(action)) {
                    onFinished(false, "Stopped at step ${index + 1}/${routine.actions.size}: target not found or action failed.")
                    return@Thread
                }
            }
            onFinished(true, "Completed ${routine.actions.size} steps.")
        }.start()
    }

    private fun perform(action: RecordedAction): Boolean {
        val root = rootInActiveWindow ?: return false
        if (action.packageName != null && root.packageName?.toString() != action.packageName) return false

        val node = findTarget(root, action) ?: return false
        return when (action.type) {
            ActionType.CLICK -> node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            ActionType.TYPE_TEXT -> {
                if (isSensitive(node)) return false
                val args = android.os.Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        action.text ?: ""
                    )
                }
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
            ActionType.SCROLL -> node.performAction(
                if (action.scrollForward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            )
            else -> true
        }
    }

    private fun findTarget(root: AccessibilityNodeInfo, action: RecordedAction): AccessibilityNodeInfo? {
        action.viewId?.let { id ->
            root.findAccessibilityNodeInfosByViewId(id).firstOrNull()?.let { return it }
        }
        action.contentDescription?.takeIf { it.isNotBlank() }?.let { description ->
            root.findAccessibilityNodeInfosByText(description).firstOrNull()?.let { return it }
        }
        action.text?.takeIf { it.isNotBlank() }?.let { text ->
            root.findAccessibilityNodeInfosByText(text).firstOrNull()?.let { return it }
        }
        return null
    }

    override fun onInterrupt() {
        recording = false
    }

    override fun onDestroy() {
        instance = null
        recording = false
        super.onDestroy()
    }
}
