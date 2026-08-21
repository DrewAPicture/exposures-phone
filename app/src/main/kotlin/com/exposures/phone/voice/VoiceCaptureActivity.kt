package com.exposures.phone.voice

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.exposures.phone.ExposuresApplication
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * App Actions fulfillment target for "save exposure" voice capture (see
 * exp--google-assistant-capture-plan.md). Exported, invisible (`Theme.NoDisplay`), reached via the
 * `custom.actions.intent.CREATE_EXPOSURE` capability's `<url-template>` deep link declared in
 * `res/xml/shortcuts.xml` — a plain `ACTION_VIEW`/`BROWSABLE` intent, since custom App Actions
 * intents only fulfill via URL-template deep links, not Intent extras. Assistant hands off with no
 * return channel for a spoken response, so this speaks its own result via [TextToSpeech] rather
 * than returning an activity result.
 */
class VoiceCaptureActivity : ComponentActivity() {

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        val shutterSpeedText = uri?.getQueryParameter("shutterSpeed")
        val lensNameText = uri?.getQueryParameter("lensName")
        val apertureText = uri?.getQueryParameter("aperture")
        val isoText = uri?.getQueryParameter("isoUsed")

        val container = (application as ExposuresApplication).container
        val handler = VoiceCaptureRequestHandler(
            container.repository,
            container.dataLayerClient,
            container.createExposureAckBroadcaster,
        )

        lifecycleScope.launch {
            val result = handler.handle(shutterSpeedText, lensNameText, apertureText, isoText)
            speak(result)
        }
    }

    private fun speak(text: String) {
        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.SUCCESS) {
                finish()
                return@TextToSpeech
            }
            tts?.language = Locale.getDefault()
            tts?.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) = finish()

                    @Deprecated("Deprecated in Java, still the only overload on the minSdk API level")
                    override fun onError(utteranceId: String?) = finish()
                },
            )
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private companion object {
        const val UTTERANCE_ID = "voice-capture-result"
    }
}
