package app.binder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object ImageCache {
    private val cache = object : LruCache<String, ImageBitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4 / 1024
    }
    fun get(key: String): ImageBitmap? = cache.get(key)
    fun put(key: String, v: ImageBitmap) { cache.put(key, v) }
}

private fun decodeFile(f: File, maxPx: Int): ImageBitmap? = runCatching {
    val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(f.path, b)
    var s = 1
    while (maxOf(b.outWidth, b.outHeight) / (s * 2) >= maxPx) s *= 2
    BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })?.asImageBitmap()
}.getOrNull()

/** Loads a saved picture off the main thread. Returns null until it is ready. */
@Composable
fun rememberImage(name: String?, maxPx: Int = 500): ImageBitmap? {
    if (name == null) return null
    val dir = LocalContext.current.filesDir
    val key = "$name@$maxPx"
    val state = produceState<ImageBitmap?>(initialValue = ImageCache.get(key), key) {
        if (value == null) {
            val bmp = withContext(Dispatchers.IO) { decodeFile(File(File(dir, "images"), name), maxPx) }
            if (bmp != null) {
                ImageCache.put(key, bmp)
                value = bmp
            }
        }
    }
    return state.value
}

private fun decodeBytes(b: ByteArray, maxPx: Int): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(b, 0, b.size, bounds)
    var s = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (s * 2) >= maxPx) s *= 2
    BitmapFactory.decodeByteArray(b, 0, b.size, BitmapFactory.Options().apply { inSampleSize = s })?.asImageBitmap()
}.getOrNull()

/** A small picture from the internet, for the rows of search results. Only used while fetching is on. */
@Composable
fun rememberRemoteImage(url: String, maxPx: Int = 200): ImageBitmap? {
    if (url.isEmpty()) return null
    val key = "url:$url@$maxPx"
    val state = produceState<ImageBitmap?>(initialValue = ImageCache.get(key), key) {
        if (value == null) {
            val bmp = withContext(Dispatchers.IO) {
                runCatching { decodeBytes(Http.getBytes(url, 2_000_000), maxPx) }.getOrNull()
            }
            if (bmp != null) {
                ImageCache.put(key, bmp)
                value = bmp
            }
        }
    }
    return state.value
}

/** Downloads a cover into files/images as a JPG, scaled down the same way as importImage. Returns the file name, or null. */
fun downloadCover(ctx: Context, url: String): String? = runCatching {
    val bytes = Http.getBytes(url, 6_000_000)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1600) sample *= 2
    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    if (bmp == null) {
        null
    } else {
        val dir = File(ctx.filesDir, "images").apply { mkdirs() }
        val name = UUID.randomUUID().toString() + ".jpg"
        File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        name
    }
}.getOrNull()

/** Copies a picked picture into the app (scaled down, rotated upright). Returns the new file name. */
fun importImage(ctx: Context, uri: Uri): String? = runCatching {
    val dir = File(ctx.filesDir, "images").apply { mkdirs() }
    val cr = ctx.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1600) sample *= 2
    val bmp = cr.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    }
    if (bmp == null) {
        null
    } else {
        val rot = cr.openInputStream(uri)?.use { s ->
            when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val upright = if (rot == 0f) bmp
        else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rot) }, true)
        val name = UUID.randomUUID().toString() + ".jpg"
        File(dir, name).outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        name
    }
}.getOrNull()
