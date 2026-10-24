package me.centralhardware.znatoki.telegram.statistic.app

/** Telegram's Mini App bridge, as far as the app uses it; only inside Telegram's webview. */
interface Telegram {
    /** Signed launch data; the server checks it on every request. */
    val initData: String
    val platform: String
    val dark: Boolean
    fun close()
    fun openLink(url: String)
    fun backButton(visible: Boolean)
    fun onBack(cb: () -> Unit)
    fun haptic(success: Boolean)
    /** Telegram's own file download, where the client has it (Bot API 8.0); false when it doesn't. */
    fun downloadFile(url: String, name: String, started: () -> Unit): Boolean
}

/** A picked photo, already shrunk to a JPEG: the bytes to upload. */
class Photo(val bytes: ByteArray)

/** What differs between the browser and the Android app. */
interface Platform {
    val telegram: Telegram?
    /** Where the API lives: empty for the page's own origin, `https://host` in the app. */
    val apiBase: String
    /** The name sent with an access request: Telegram's platform, `web` or `android`. */
    val name: String
    /** Deep link parameters (`tab=report&month=2026-10`, `slot=12`), from the page's hash or the app's intent. */
    val deepLink: String
    fun load(key: String): String?
    fun save(key: String, value: String?)
    fun openUrl(url: String)
    /** Saves a generated file from the API's one-time link. */
    fun download(url: String, name: String)
    fun pickPhoto(done: (Photo) -> Unit, failed: () -> Unit)
}

expect val platform: Platform
