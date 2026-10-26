package me.centralhardware.znatoki.telegram.statistic.app

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** The Android app's side: the API at [apiHost], prefs for storage, system apps for links and files. */
object AndroidPlatform : Platform {
    lateinit var context: Context
    /** The API's host, from the app build (`-Phost=…`). */
    var apiHost = ""
    /** Set by [MainActivity]: launches the system photo picker and reports the picked image. */
    var launchPicker: ((Uri?) -> Unit) -> Unit = { it(null) }
    var link = ""

    private val prefs by lazy { context.getSharedPreferences("classlog", Context.MODE_PRIVATE) }

    override val telegram: Telegram? = null
    override val apiBase get() = "https://$apiHost"
    override val name = "android"
    override val deepLink get() = link
    override fun load(key: String): String? = prefs.getString(key, null)
    override fun save(key: String, value: String?) = prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()

    override fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Нет приложения, чтобы открыть ссылку", Toast.LENGTH_SHORT).show()
        }
    }

    /** Generated reports come from the API's one-time /dl/ links; the system downloader saves them. */
    override fun download(url: String, name: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
        (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        Toasts.show("Загрузка началась")
    }

    override fun pickPhoto(done: (Photo) -> Unit, failed: () -> Unit) = launchPicker { uri ->
        if (uri == null) return@launchPicker
        val bytes = runCatching { shrink(uri) }.getOrNull()
        if (bytes == null) failed() else done(Photo(bytes))
    }

    /** Decodes the picked image at most 2000px on its long side and re-encodes it as JPEG, as the web build does. */
    private fun shrink(uri: Uri): ByteArray {
        val r = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        r.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 2000) sample *= 2
        val raw = r.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }!!
        val k = minOf(1.0, 2000.0 / max(raw.width, raw.height))
        val bmp = if (k < 1) Bitmap.createScaledBitmap(raw, (raw.width * k).roundToInt(), (raw.height * k).roundToInt(), true) else raw
        return ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
    }
}

actual val platform: Platform = AndroidPlatform
