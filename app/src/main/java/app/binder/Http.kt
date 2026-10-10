package app.binder

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Something went wrong talking to a source. The message is safe to show in the sheet. */
class FetchFailure(message: String, val offline: Boolean = false) : Exception(message)

/** A tiny HTTP helper: GET only, timeouts, a User-Agent, size limit. Always call it off the main thread. */
object Http {
    const val USER_AGENT = "Binder/1.0.0 (Android; personal list app)"

    fun getBytes(url: String, maxBytes: Int = 8_000_000): ByteArray {
        val c = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: Exception) {
            throw FetchFailure("That link doesn't look right.")
        }
        c.connectTimeout = 8_000
        c.readTimeout = 10_000
        c.requestMethod = "GET"
        c.setRequestProperty("User-Agent", USER_AGENT)
        c.setRequestProperty("Accept", "*/*")
        try {
            val code = c.responseCode
            if (code == 403 || code == 429) throw FetchFailure("The source is busy. Try again in a minute.")
            if (code !in 200..299) throw FetchFailure("The source answered with error $code.")
            val out = ByteArrayOutputStream()
            c.inputStream.use { ins ->
                val buf = ByteArray(8192)
                while (true) {
                    val n = ins.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    if (out.size() > maxBytes) throw FetchFailure("The answer was too big.")
                }
            }
            return out.toByteArray()
        } catch (e: FetchFailure) {
            throw e
        } catch (e: java.io.IOException) {
            // no connection, timeout, DNS problem …
            throw FetchFailure("Couldn't reach the internet.", offline = true)
        } finally {
            c.disconnect()
        }
    }

    fun get(url: String): String = String(getBytes(url), Charsets.UTF_8)
}

/** Keeps us under a source's request limit: at most [max] calls in any [windowMs]. */
class RateGate(private val max: Int, private val windowMs: Long, private val now: () -> Long = System::currentTimeMillis) {
    private val times = ArrayDeque<Long>()

    @Synchronized
    fun check() {
        val t = now()
        while (times.isNotEmpty() && t - times.first() >= windowMs) times.removeFirst()
        if (times.size >= max) throw FetchFailure("Too many searches in a row. Wait a moment and try again.")
        times.addLast(t)
    }
}
