@file:OptIn(ExperimentalWasmJsInterop::class)

package me.centralhardware.znatoki.telegram.statistic.app

import kotlin.io.encoding.Base64
import kotlin.js.ExperimentalWasmJsInterop

// The browser side, through small JS snippets (the same code serves the Wasm build and its JS fallback).

private fun tgPresent(): Boolean = js("!!(window.Telegram && window.Telegram.WebApp && window.Telegram.WebApp.initData !== undefined && window.Telegram.WebApp.platform !== 'unknown')")
private fun tgInitData(): String = js("window.Telegram.WebApp.initData || ''")
private fun tgPlatform(): String = js("window.Telegram.WebApp.platform || 'web'")
private fun tgDark(): Boolean = js("window.Telegram.WebApp.colorScheme === 'dark'")
private fun tgReady(): Unit = js("{ window.Telegram.WebApp.ready(); window.Telegram.WebApp.expand(); }")
private fun tgClose(): Unit = js("window.Telegram.WebApp.close()")
private fun tgOpenLink(url: String): Unit = js("window.Telegram.WebApp.openLink(url)")
private fun tgBack(visible: Boolean): Unit = js("visible ? window.Telegram.WebApp.BackButton.show() : window.Telegram.WebApp.BackButton.hide()")
private fun tgOnBack(cb: () -> Unit): Unit = js("window.Telegram.WebApp.BackButton.onClick(cb)")
private fun tgHaptic(kind: String): Unit = js("{ var h = window.Telegram.WebApp.HapticFeedback; if (h) h.notificationOccurred(kind); }")
private fun tgCanDownload(): Boolean = js("window.Telegram.WebApp.isVersionAtLeast('8.0')")
private fun tgDownload(url: String, name: String, cb: () -> Unit): Unit =
    js("window.Telegram.WebApp.downloadFile({ url: url, file_name: name }, function (ok) { if (ok) cb(); })")

private fun apiOverride(): String = js("window.CLASSLOG_API || window.location.origin")
private fun hash(): String = js("window.location.hash.replace(/^#/, '')")
private fun storageGet(key: String): String? = js("(function () { try { return window.localStorage.getItem(key); } catch (e) { return null; } })()")
private fun storageSet(key: String, value: String?): Unit =
    js("{ try { if (value == null) window.localStorage.removeItem(key); else window.localStorage.setItem(key, value); } catch (e) {} }")
private fun openWindow(url: String): Unit = js("{ if (/^https?:/.test(url)) window.open(url, '_blank'); else window.location.href = url; }")
private fun navigate(url: String): Unit = js("window.location.href = url")

/** Opens the file picker; the picked image is shrunk to at most 2000px and handed back as base64 JPEG. */
private fun pickImage(done: (String) -> Unit, failed: () -> Unit): Unit = js("""{
    var inp = document.createElement('input');
    inp.type = 'file'; inp.accept = 'image/*';
    inp.onchange = function () {
        var f = inp.files && inp.files[0];
        if (!f) return;
        var src = URL.createObjectURL(f);
        var img = new Image();
        img.onload = function () {
            var k = Math.min(1, 2000 / Math.max(img.width, img.height));
            var c = document.createElement('canvas');
            c.width = Math.round(img.width * k); c.height = Math.round(img.height * k);
            c.getContext('2d').drawImage(img, 0, 0, c.width, c.height);
            URL.revokeObjectURL(src);
            done(c.toDataURL('image/jpeg', 0.85).split(',')[1]);
        };
        img.onerror = function () { URL.revokeObjectURL(src); failed(); };
        img.src = src;
    };
    inp.click();
}""")

private object WebTelegram : Telegram {
    override val initData get() = tgInitData()
    override val platform get() = tgPlatform()
    override val dark get() = tgDark()
    override fun close() = tgClose()
    override fun openLink(url: String) = tgOpenLink(url)
    override fun backButton(visible: Boolean) = tgBack(visible)
    override fun onBack(cb: () -> Unit) = tgOnBack(cb)
    override fun haptic(success: Boolean) = tgHaptic(if (success) "success" else "error")
    override fun downloadFile(url: String, name: String, started: () -> Unit): Boolean {
        if (!tgCanDownload()) return false
        tgDownload(url, name, started)
        return true
    }
}

private object Web : Platform {
    override val telegram: Telegram? = if (tgPresent()) WebTelegram.also { tgReady() } else null
    override val apiBase = apiOverride()
    override val name get() = telegram?.platform ?: "web"
    override val deepLink get() = hash()
    override fun load(key: String) = storageGet(key)
    override fun save(key: String, value: String?) = storageSet(key, value)
    override fun openUrl(url: String) = openWindow(url)
    override fun download(url: String, name: String) = navigate(url)
    override fun pickPhoto(done: (Photo) -> Unit, failed: () -> Unit) =
        pickImage({ b64 -> done(Photo(Base64.decode(b64))) }, failed)
}

actual val platform: Platform = Web
