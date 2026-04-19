package rocks.claudiusthebot.watchbuddy.protocol

import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Parses the newline-delimited JSON stream from the Claude desktop app and
 * produces structured events. Also builds ack / decision replies.
 *
 * The desktop fragments lines at the BLE MTU boundary, so we accumulate bytes
 * until '\n' and parse each complete line.
 */
class BuddyProtocol(
    private val listener: Listener
) {
    interface Listener {
        fun onHeartbeat(hb: Heartbeat)
        fun onTurnEvent(role: String, content: JSONArray)
        fun onTime(epochSec: Long, tzOffsetSec: Int)
        fun onOwner(name: String)
        fun onSetName(name: String)
        fun onUnpair()
        fun onStatusRequest()
        fun onCharBegin(name: String, total: Long)
        fun onFile(path: String, size: Long)
        fun onChunk(dataB64: String)
        fun onFileEnd()
        fun onCharEnd()
        fun onUnknown(line: String) {}
    }

    private val buf = StringBuilder()

    fun onBytes(bytes: ByteArray, len: Int = bytes.size) {
        // Bytes from BLE are UTF-8; append and split on newline.
        buf.append(String(bytes, 0, len, Charsets.UTF_8))
        while (true) {
            val nl = buf.indexOf('\n')
            if (nl < 0) break
            val line = buf.substring(0, nl).trim()
            buf.delete(0, nl + 1)
            if (line.isNotEmpty()) dispatch(line)
        }
        if (buf.length > MAX_BUFFER) {
            Log.w(TAG, "protocol buffer exceeded $MAX_BUFFER bytes — dropping")
            buf.setLength(0)
        }
    }

    private fun dispatch(line: String) {
        val obj = try {
            JSONObject(line)
        } catch (e: JSONException) {
            Log.w(TAG, "bad json: $line")
            listener.onUnknown(line)
            return
        }

        // heartbeat has no cmd field — detect by shape
        val cmd = obj.optString("cmd", "")
        val evt = obj.optString("evt", "")

        when {
            obj.has("time") -> {
                val arr = obj.optJSONArray("time") ?: return
                listener.onTime(arr.optLong(0, 0L), arr.optInt(1, 0))
            }
            cmd == "owner" -> listener.onOwner(obj.optString("name", ""))
            cmd == "name" -> listener.onSetName(obj.optString("name", ""))
            cmd == "unpair" -> listener.onUnpair()
            cmd == "status" -> listener.onStatusRequest()
            cmd == "char_begin" -> listener.onCharBegin(
                obj.optString("name", ""),
                obj.optLong("total", 0L)
            )
            cmd == "file" -> listener.onFile(
                obj.optString("path", ""),
                obj.optLong("size", 0L)
            )
            cmd == "chunk" -> listener.onChunk(obj.optString("d", ""))
            cmd == "file_end" -> listener.onFileEnd()
            cmd == "char_end" -> listener.onCharEnd()
            evt == "turn" -> {
                val role = obj.optString("role", "assistant")
                val content = obj.optJSONArray("content") ?: JSONArray()
                listener.onTurnEvent(role, content)
            }
            // Heartbeat heuristic: has at least one of these fields.
            obj.has("total") || obj.has("msg") || obj.has("entries") ||
            obj.has("tokens") || obj.has("prompt") -> {
                listener.onHeartbeat(Heartbeat.parse(obj))
            }
            else -> listener.onUnknown(line)
        }
    }

    companion object {
        private const val TAG = "BuddyProtocol"
        private const val MAX_BUFFER = 64 * 1024
    }
}

/** Helpers for building outgoing JSON lines. */
object BuddyMessages {
    fun ack(cmd: String, ok: Boolean = true, n: Long = 0L, error: String? = null): String {
        val o = JSONObject()
        o.put("ack", cmd)
        o.put("ok", ok)
        o.put("n", n)
        if (error != null) o.put("error", error)
        return o.toString() + "\n"
    }

    fun statusAck(
        name: String,
        sec: Boolean,
        batPct: Int?, batMv: Int?, batMa: Int?, usb: Boolean?,
        upSec: Long, heapBytes: Long,
        appr: Int, deny: Int, vel: Int, nap: Int, lvl: Int
    ): String {
        val o = JSONObject()
        o.put("ack", "status")
        o.put("ok", true)
        o.put("n", 0)
        val data = JSONObject()
        data.put("name", name)
        data.put("sec", sec)
        val bat = JSONObject()
        if (batPct != null) bat.put("pct", batPct)
        if (batMv  != null) bat.put("mV", batMv)
        if (batMa  != null) bat.put("mA", batMa)
        if (usb    != null) bat.put("usb", usb)
        data.put("bat", bat)
        val sys = JSONObject()
        sys.put("up", upSec)
        sys.put("heap", heapBytes)
        data.put("sys", sys)
        val stats = JSONObject()
        stats.put("appr", appr)
        stats.put("deny", deny)
        stats.put("vel", vel)
        stats.put("nap", nap)
        stats.put("lvl", lvl)
        data.put("stats", stats)
        o.put("data", data)
        return o.toString() + "\n"
    }

    fun permission(id: String, decision: String): String {
        val o = JSONObject()
        o.put("cmd", "permission")
        o.put("id", id)
        o.put("decision", decision) // "once" or "deny"
        return o.toString() + "\n"
    }
}
