package com.samsunghack.morrow

data class Routine(
    val id: String,
    val name: String,
    val phrase: String,
    val actions: List<RecordedAction> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastRun: Long? = null
)

data class RecordedAction(
    val type: ActionType,
    val text: String? = null,
    val className: String? = null,
    val viewId: String? = null,
    val packageName: String? = null,
    val contentDescription: String? = null,
    val boundsLeft: Int? = null,
    val boundsTop: Int? = null,
    val boundsRight: Int? = null,
    val boundsBottom: Int? = null,
    val delayMs: Long = 0L
)

enum class ActionType { CLICK, TYPE_TEXT, SCROLL, FOCUS, WAIT }
