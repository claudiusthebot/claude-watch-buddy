package rocks.claudiusthebot.watchbuddy.protocol

import org.json.JSONObject

/** A permission prompt pulled out of the heartbeat. */
data class Prompt(
    val id: String,
    val tool: String,
    val hint: String
)

/**
 * Heartbeat snapshot from the Claude desktop. One object per line over BLE.
 * See REFERENCE.md in the ESP32 buddy repo for the full field list.
 */
data class Heartbeat(
    val total: Int = 0,
    val running: Int = 0,
    val waiting: Int = 0,
    val msg: String = "",
    val entries: List<String> = emptyList(),
    val tokens: Long = 0L,
    val tokensToday: Long = 0L,
    val prompt: Prompt? = null
) {
    companion object {
        fun parse(o: JSONObject): Heartbeat {
            val entriesJson = o.optJSONArray("entries")
            val entries = if (entriesJson == null) emptyList() else buildList {
                for (i in 0 until entriesJson.length()) add(entriesJson.optString(i, ""))
            }
            val promptObj = o.optJSONObject("prompt")
            val prompt = if (promptObj != null) {
                Prompt(
                    id = promptObj.optString("id", ""),
                    tool = promptObj.optString("tool", ""),
                    hint = promptObj.optString("hint", "")
                )
            } else null
            return Heartbeat(
                total = o.optInt("total", 0),
                running = o.optInt("running", 0),
                waiting = o.optInt("waiting", 0),
                msg = o.optString("msg", ""),
                entries = entries,
                tokens = o.optLong("tokens", 0L),
                tokensToday = o.optLong("tokens_today", 0L),
                prompt = prompt
            )
        }
    }
}
