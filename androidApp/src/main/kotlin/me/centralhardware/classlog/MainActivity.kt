package me.centralhardware.classlog

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import me.centralhardware.znatoki.telegram.statistic.app.AndroidPlatform
import me.centralhardware.znatoki.telegram.statistic.app.App
import me.centralhardware.znatoki.telegram.statistic.app.ClassLogApp
import me.centralhardware.znatoki.telegram.statistic.app.applyDeepLink

/** The native app: the same Compose UI as the Mini App, talking to the API directly. */
class MainActivity : ComponentActivity() {

    private var picked: ((Uri?) -> Unit)? = null
    private val picker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        picked?.invoke(uri)
        picked = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidPlatform.context = applicationContext
        AndroidPlatform.apiHost = BuildConfig.API_HOST
        AndroidPlatform.launchPicker = { cb ->
            picked = cb
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        // Deep links carry the Mini App's hash parameters in the fragment: …#tab=report&month=2026-10.
        AndroidPlatform.link = intent.data?.fragment.orEmpty()
        enableEdgeToEdge()
        setContent { ClassLogApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.fragment?.let { if (App.boot != null) applyDeepLink(it) }
    }
}
