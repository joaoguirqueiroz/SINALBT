package br.com.vertice81.sinalbt.bluetooth

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import br.com.vertice81.sinalbt.model.EventRecord
import org.json.JSONArray
import org.json.JSONObject

/** Histórico local. Nenhuma transmissão remota. */
class EventStore(context: Context) {
    private val prefs = context.getSharedPreferences("sinalbt_history", Context.MODE_PRIVATE)
    val records = mutableStateListOf<EventRecord>()

    init {
        runCatching {
            val array = JSONArray(prefs.getString("events", "[]"))
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                records.add(EventRecord(
                    o.optLong("time"), o.optString("type"), o.optString("device"),
                    o.optString("address"), o.optString("message")
                ))
            }
        }
    }

    fun add(type: String, device: String, address: String, message: String) {
        records.add(0, EventRecord(System.currentTimeMillis(), type, device, address, message))
        while (records.size > 100) records.removeAt(records.lastIndex)
        save()
    }

    fun clear() {
        records.clear()
        save()
    }

    fun toJson(): String {
        val array = JSONArray()
        records.forEach {
            array.put(JSONObject().apply {
                put("time", it.time)
                put("type", it.type)
                put("device", it.device)
                put("address", it.address)
                put("message", it.message)
            })
        }
        return JSONObject().apply {
            put("application", "SINAL BT")
            put("schema", 1)
            put("notice", "Logs locais, sem captura de audio nem comprovacao de CVE")
            put("events", array)
        }.toString(2)
    }

    private fun save() {
        prefs.edit().putString("events", toJsonEvents()).apply()
    }

    private fun toJsonEvents(): String {
        val array = JSONArray()
        records.forEach {
            array.put(JSONObject().apply {
                put("time", it.time)
                put("type", it.type)
                put("device", it.device)
                put("address", it.address)
                put("message", it.message)
            })
        }
        return array.toString()
    }
}
