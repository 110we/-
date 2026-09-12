package com.kalidroid.bridge

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.kalidroid.KaliDroidApp
import com.kalidroid.permission.ExecutorMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class NativeExecutorBridge(
    private val scope: CoroutineScope
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private var socket: WebSocket? = null
    private var job: Job? = null
    @Volatile var connected: Boolean = false
        private set

    fun connect(url: String) {
        disconnect()
        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                connected = true
                webSocket.send("""{"type":"hello","role":"android"}""")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val obj = runCatching { gson.fromJson(text, JsonObject::class.java) }.getOrNull() ?: return
                if (obj.get("type")?.asString != "exec") return
                val id = obj.get("id")?.asString.orEmpty()
                val command = obj.get("command")?.asString.orEmpty()
                job = scope.launch(Dispatchers.IO) {
                    val requested = obj.get("mode")?.asString ?: "NORMAL"
                    val mode = runCatching { ExecutorMode.valueOf(requested) }.getOrDefault(ExecutorMode.NORMAL)
                    val executor = KaliDroidApp.instance.permissionManager.executorFor(mode)
                    val result = executor.exec(command)
                    com.kalidroid.utils.AuditLogger.log(
                        mode = mode.name,
                        command = command,
                        allowed = !result.denied,
                        denied = result.denied,
                        ok = result.ok,
                        note = "via native ws"
                    )
                    val payload = mapOf(
                        "type" to "result",
                        "id" to id,
                        "ok" to result.ok,
                        "stdout" to result.stdout,
                        "stderr" to result.stderr,
                        "denied" to result.denied,
                        "executor" to mode.name
                    )
                    webSocket.send(gson.toJson(payload))
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                connected = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connected = false
            }
        })
    }

    fun disconnect() {
        job?.cancel()
        socket?.close(1000, "bye")
        socket = null
        connected = false
    }
}
