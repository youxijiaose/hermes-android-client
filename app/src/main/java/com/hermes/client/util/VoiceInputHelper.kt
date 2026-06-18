package com.hermes.client.util

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class VoiceInputHelper(
    private val activity: Activity,
    private val onSpeechResult: (String) -> Unit,
    private val onSpeechError: (Int) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    init {
        setupSpeechRecognizer()
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(activity.applicationContext)) {
            onSpeechError(-1)
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                }

                override fun onBeginningOfSpeech() {
                    // User started speaking
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Audio level changed
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // Partial audio data
                }

                override fun onEndOfSpeech() {
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                    onSpeechError(error)
                }

                override fun onResults(results: android.os.Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let { result ->
                        onSpeechResult(result)
                    }
                }

                override fun onPartialResults(partialResults: android.os.Bundle?) {
                    // Partial recognition results
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Reserved for future events
                }
            })
        }
    }

    fun startListening() {
        if (!hasPermission()) {
            requestPermission()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            onSpeechError(-1)
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            PERMISSION_REQUEST_RECORD_AUDIO
        )
    }

    companion object {
        private const val PERMISSION_REQUEST_RECORD_AUDIO = 1001
    }
}

// TTS Helper for speaking responses
class TTSHelper(
    private val activity: Activity,
    private val onSpeakingComplete: () -> Unit = {}
) {
    private var textToSpeech: TextToSpeech? = null
    private var isSpeaking = false

    init {
        textToSpeech = TextToSpeech(activity) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to default language
                    textToSpeech?.setLanguage(Locale.ENGLISH)
                }
            }
        }
    }

    fun speak(text: String) {
        if (textToSpeech == null) return
        
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                onSpeakingComplete()
            }

            override fun onError(utteranceId: String?) {
                isSpeaking = false
            }

            @Deprecated("Deprecated in Java")
            override fun onStarterDone(utteranceId: String?) {
                // No-op
            }
        })

        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun stop() {
        textToSpeech?.stop()
        isSpeaking = false
    }

    fun isSpeaking(): Boolean = isSpeaking

    fun destroy() {
        stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
