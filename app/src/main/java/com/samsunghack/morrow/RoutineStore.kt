package com.samsunghack.morrow

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class RoutineStore(context: Context) {
    private val prefs = context.getSharedPreferences("morrow_routines", Context.MODE_PRIVATE)

    fun save(routine: Routine) {
        val routines = load().filterNot { it.id == routine.id } + routine
        write(routines)
    }

    fun load(): List<Routine> {
        val root = JSONArray(prefs.getString("routines", "[]"))
        return buildList {
            for (i in 0 until root.length()) {
                val obj = root.getJSONObject(i)
                val actions = obj.optJSONArray("actions") ?: JSONArray()
                add(
                    Routine(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        phrase = obj.getString("phrase"),
                        actions = buildList {
                            for (j in 0 until actions.length()) {
                                val a = actions.getJSONObject(j)
                                add(
                                    RecordedAction(
                                        type = ActionType.valueOf(a.getString("type")),
                                        text = a.optString("text").takeIf { it.isNotBlank() },
                                        className = a.optString("className").takeIf { it.isNotBlank() },
                                        viewId = a.optString("viewId").takeIf { it.isNotBlank() },
                                        packageName = a.optString("packageName").takeIf { it.isNotBlank() },
                                        contentDescription = a.optString("contentDescription").takeIf { it.isNotBlank() },
                                        boundsLeft = a.optInt("boundsLeft").takeIf { a.has("boundsLeft") },
                                        boundsTop = a.optInt("boundsTop").takeIf { a.has("boundsTop") },
                                        boundsRight = a.optInt("boundsRight").takeIf { a.has("boundsRight") },
                                        boundsBottom = a.optInt("boundsBottom").takeIf { a.has("boundsBottom") },
                                        delayMs = a.optLong("delayMs", 0L),
                                        scrollForward = a.optBoolean("scrollForward", true)
                                    )
                                )
                            }
                        },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        lastRun = obj.optLong("lastRun", -1L).takeIf { it >= 0 }
                    )
                )
            }
        }
    }

    fun count(): Int = load().size

    fun markRun(id: String) {
        load().find { it.id == id }?.let { save(it.copy(lastRun = System.currentTimeMillis())) }
    }

    private fun write(routines: List<Routine>) {
        val root = JSONArray()
        routines.forEach { routine ->
            val actions = JSONArray()
            routine.actions.forEach { action ->
                actions.put(
                    JSONObject()
                        .put("type", action.type.name)
                        .put("text", action.text)
                        .put("className", action.className)
                        .put("viewId", action.viewId)
                        .put("packageName", action.packageName)
                        .put("contentDescription", action.contentDescription)
                        .put("boundsLeft", action.boundsLeft)
                        .put("boundsTop", action.boundsTop)
                        .put("boundsRight", action.boundsRight)
                        .put("boundsBottom", action.boundsBottom)
                        .put("delayMs", action.delayMs)
                        .put("scrollForward", action.scrollForward)
                )
            }
            root.put(
                JSONObject()
                    .put("id", routine.id)
                    .put("name", routine.name)
                    .put("phrase", routine.phrase)
                    .put("createdAt", routine.createdAt)
                    .put("lastRun", routine.lastRun ?: JSONObject.NULL)
                    .put("actions", actions)
            )
        }
        prefs.edit().putString("routines", root.toString()).apply()
    }
}
