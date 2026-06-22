package com.hermes.client.api

import com.hermes.client.model.*
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * WebSocket JSON-RPC client for the Hermes Dashboard Gateway.
 *
 * Connects to ws://host:port/api/ws?token=TOKEN, sends JSON-RPC 2.0 requests,
 * and dispatches server-pushed events (message.delta, thinking.delta, etc.)
 * to registered listeners.
 *
 * Wire format (identical to the Ink TUI stdio protocol):
 *   Request:  {"jsonrpc":"2.0", "id":"w1", "method":"prompt.submit", "params":{...}}
 *   Response: {"jsonrpc":"2.0", "id":"w1", "result":{...}}
 *   Event:    {"jsonrpc":"2.0", "method":"event", "params":{"type":"message.delta", "session_id":"x", "payload":{...}}}
 */
class GatewayClient {

    companion object {
        private const val TAG = "GatewayClient"
        private const val REQUEST_TIMEOUT_MS = 120_000L
    }

    // --- State ---
    private var ws: WebSocket? = null
    private var reqId = 0
    private val pending = ConcurrentHashMap<String, PendingRequest>()
    private val eventListeners = mutableMapOf<String, MutableSet<(GatewayEvent) -> Unit>>()
    private val wildcardListeners = mutableSetOf<(GatewayEvent) -> Unit>()
    private var stateListeners = mutableSetOf<(State) -> Unit>()
    private var _state = State.Idle

    enum class State { Idle, Connecting, Open, Closed, Error }

    val state: State get() = _state

    // --- Connection ---

    /**
     * Connect to the dashboard gateway.
     *
     * @param host Dashboard host (e.g. "http://127.0.0.1:9119")
     * @param token Session token. If null, auto-fetched from the dashboard index.html.
     */
    suspend fun connect(host: String, token: String? = null) {
        if (_state == State.Open || _state == State.Connecting) return
        setState(State.Connecting)

        val resolvedToken = token ?: fetchToken(host)
        if (resolvedToken.isNullOrEmpty()) {
            setState(State.Error)
            throw IOException("No session token available — visit $host in a browser first")
        }

        val wsUrl = host
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .trimEnd('/') + "/api/ws?token=" + java.net.URLEncoder.encode(resolvedToken, "UTF-8")

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // no timeout for streaming
            .build()

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        // Use suspendCancellableCoroutine to bridge the OkHttp callback to coroutines
        suspendCancellableCoroutine<Unit> { cont ->
            client.newWebSocket(request, object : okhttp3.WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    ws = webSocket
                    setState(State.Open)
                    if (cont.isActive) {
                        cont.resume(Unit)
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    dispatch(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(1000, null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    ws = null
                    rejectAllPending(IOException("WebSocket closed: $code $reason"))
                    setState(State.Closed)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    ws = null
                    rejectAllPending(t)
                    setState(State.Error)
                    if (cont.isActive) {
                        cont.resumeWithException(t)
                    }
                }
            })
        }
    }

    fun close() {
        ws?.close(1000, "client close")
        ws = null
        rejectAllPending(IOException("WebSocket closed by client"))
        setState(State.Closed)
    }

    // --- JSON-RPC Request ---

    /**
     * Send a JSON-RPC request and return the result as a JSONObject.
     *
     * Suspends until the response arrives (or timeout).
     */
    suspend fun request(method: String, params: Map<String, Any?> = emptyMap()): JSONObject? {
        val id = "w${++reqId}"
        val body = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", id)
            put("method", method)
            put("params", JSONObject(params))
        }

        return suspendCancellableCoroutine { continuation ->
            pending[id] = PendingRequest(
                resolve = { resultStr ->
                    if (continuation.isActive) {
                        continuation.resume(resultStr?.let { JSONObject(it) })
                    }
                },
                reject = { error ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
            )

            try {
                ws?.send(body.toString())
            } catch (e: Exception) {
                pending.remove(id)
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    // --- Event Listeners ---

    fun on(type: String, listener: (GatewayEvent) -> Unit) {
        eventListeners.getOrPut(type) { mutableSetOf() }.add(listener)
    }

    fun off(type: String, listener: (GatewayEvent) -> Unit) {
        eventListeners[type]?.remove(listener)
    }

    /** Listen to ALL events regardless of type. */
    fun onAny(listener: (GatewayEvent) -> Unit) {
        wildcardListeners.add(listener)
    }

    fun offAny(listener: (GatewayEvent) -> Unit) {
        wildcardListeners.remove(listener)
    }

    fun onState(callback: (State) -> Unit) {
        stateListeners.add(callback)
        callback(_state)
    }

    fun offState(callback: (State) -> Unit) {
        stateListeners.remove(callback)
    }

    // --- Internal ---

    private fun setState(s: State) {
        if (_state == s) return
        _state = s
        for (cb in stateListeners) cb(s)
    }

    private fun dispatch(text: String) {
        try {
            val msg = JSONObject(text)

            // Response to a pending request (has "id" and either "result" or "error")
            if (msg.has("id")) {
                val id = msg.getString("id")
                val p = pending.remove(id)
                if (p != null) {
                    if (msg.has("error")) {
                        val err = msg.getJSONObject("error")
                        p.reject(IOException(err.optString("message", "request failed")))
                    } else {
                        val resultObj = msg.opt("result")
                        val resultStr = when (resultObj) {
                            is JSONObject -> resultObj.toString()
                            null -> null
                            else -> resultObj.toString()
                        }
                        p.resolve(resultStr)
                    }
                }
                return
            }

            // Server-pushed event
            if (msg.optString("method") == "event" && msg.has("params")) {
                val params = msg.getJSONObject("params")
                val eventType = params.optString("type", "")
                if (eventType.isEmpty()) return

                val sessionId = params.optString("session_id")
                val payloadObj = params.opt("payload")
                val payloadStr = if (payloadObj is JSONObject) payloadObj.toString() else null

                val event = GatewayEvent(
                    type = eventType,
                    sessionId = sessionId.ifEmpty { null },
                    payload = payloadStr
                )

                // Fire type-specific listeners (toList to avoid ConcurrentModificationException)
                eventListeners[eventType]?.toList()?.forEach { it(event) }
                // Fire wildcard listeners
                wildcardListeners.toList().forEach { it(event) }
            }
        } catch (e: Exception) {
            // Malformed frame - ignore
        }
    }

    private fun rejectAllPending(error: Throwable) {
        for ((_, p) in pending) {
            p.reject(error)
        }
        pending.clear()
    }

    /**
     * Fetch the session token from the dashboard's index.html page.
     * The token is embedded in a `<script>` tag:
     *   window.__HERMES_SESSION_TOKEN__="TOKEN_VALUE"
     */
    private fun fetchToken(host: String): String? {
        return try {
            val url = host.trimEnd('/') + "/"
            val request = Request.Builder().url(url).build()
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return null

            val regex = Regex("""__HERMES_SESSION_TOKEN__="([^"]+)"""")
            regex.find(html)?.groupValues?.getOrNull(1)
        } catch (e: Exception) {
            null
        }
    }

    private data class PendingRequest(
        val resolve: (String?) -> Unit,
        val reject: (Throwable) -> Unit
    )
}

/**
 * A single server-pushed gateway event.
 *
 * Common event types:
 * - "gateway.ready" — connection established
 * - "session.info" — session metadata
 * - "message.start" / "message.delta" / "message.complete" — assistant message stream
 * - "thinking.delta" / "reasoning.delta" — thinking/reasoning tokens
 * - "tool.start" / "tool.progress" / "tool.complete" — tool calls
 * - "approval.request" / "clarify.request" / "sudo.request" / "secret.request" — interactive requests
 * - "error" — gateway error
 */
data class GatewayEvent(
    val type: String,
    val sessionId: String? = null,
    val payload: String? = null
)