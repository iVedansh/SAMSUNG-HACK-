package com.samsunghack.morrow

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class RoutineStore(context: Context) {
    private val prefs = context.getSharedPreferences("morrow_routines", Context.MODE_PRIVATE)

    fun save(routine: Routine) {
        val root = JSONArray(prefs.getString("routines", "[]"))
        val obj = JSONObject()
            .put("id", routine.id)
            .put("name", routine.name)
            .put("phrase", routine.phrase)
            .put("createdAt", routine.createdAt)
        val actions = JSONArray()
        routine.actions.forEach {
            actions.put(JSONObject()
                .put("type", it.type.name)
                .put("text", it.text)
                .put("className", it.className)
                .put("viewId", it.viewId)
                .put("packageName", it.packageName)
                .put("contentDescription", it.contentDescription)
                .put("delayMs", it.delayMs))
        }
        obj.put("actions", actions)
        root.put(obj)
        prefs.edit().putString("routines", root.toString()).apply()
    }

    fun count(): Int = JSONArray(prefs.getString("routines", "[]")).length()
}
