package com.samsunghack.morrow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var teachButton: Button
    private var speechRecognizer: SpeechRecognizer? = null
    private var teaching = false
    private val store by lazy { RoutineStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.status)
        logText = findViewById(R.id.log)
        teachButton = findViewById(R.id.teachButton)

        findViewById<Button>(R.id.accessibilityButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        teachButton.setOnClickListener {
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "Enable Accessibility Service first.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } else if (!teaching) {
                showTeachDialog()
            } else {
                stopTeaching()
            }
        }

        findViewById<Button>(R.id.voiceButton).setOnClickListener { startVoiceCommand() }
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun showTeachDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 8)
        }
        val name = EditText(this).apply { hint = "Routine name" }
        val phrase = EditText(this).apply { hint = "Voice phrase, e.g. open settings" }
        layout.addView(name)
        layout.addView(phrase)

        AlertDialog.Builder(this)
            .setTitle("Teach a new routine")
            .setView(layout)
            .setPositiveButton("Start teaching") { _, _ ->
                if (name.text.isBlank() || phrase.text.isBlank()) {
                    Toast.makeText(this, "Name and voice phrase are required.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (MorrowAccessibilityService.startTeaching()) {
                    teaching = true
                    teachButton.text = "Stop Teaching"
                    teachButton.tag = Pair(name.text.toString(), phrase.text.toString())
                    logText.text = "Teaching started. Switch to the target app, perform the routine, then return and press Stop Teaching."
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun stopTeaching() {
        val metadata = teachButton.tag as? Pair<*, *>
        val name = metadata?.first?.toString().orEmpty()
        val phrase = metadata?.second?.toString().orEmpty()
        val actions = MorrowAccessibilityService.stopTeaching()
        val routine = Routine(UUID.randomUUID().toString(), name, phrase, actions)
        store.save(routine)

        teaching = false
        teachButton.text = "Teach a Routine"
        teachButton.tag = null
        logText.text = "Saved: ${routine.name}\nCaptured actions: ${actions.size}\nSaved routines: ${store.count()}"
    }

    private fun updateStatus() {
        statusText.text = if (isAccessibilityEnabled()) "Accessibility Service: Enabled"
        else "Accessibility Service: Disabled"
        if (!teaching) logText.text = "Saved routines: ${store.count()}"
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabled.split(':').any {
            it.equals(
                packageName + "/" + MorrowAccessibilityService::class.java.name,
                ignoreCase = true
            )
        }
    }

    private fun startVoiceCommand() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition is not available on this device.", Toast.LENGTH_LONG).show()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).also { recognizer ->
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    logText.text = "Listening..."
                }

                override fun onResults(results: Bundle?) {
                    val command = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    handleCommand(command)
                }

                override fun onError(error: Int) {
                    logText.text = "Speech recognition error: $error"
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell Morrow what to do")
            }
            recognizer.startListening(intent)
        }
    }

    private fun handleCommand(command: String) {
        if (command.isBlank()) {
            logText.text = "No command recognized."
            return
        }

        val match = RoutineMatcher.match(command, store.load())
        if (match == null) {
            logText.text = "Heard: $command\nNo routine matched."
            return
        }

        logText.text = "Heard: $command\nRunning: ${match.routine.name}"
        MorrowAccessibilityService.runRoutine(match.routine) { success, message ->
            runOnUiThread {
                if (success) store.markRun(match.routine.id)
                logText.text = "${if (success) "Completed" else "Failed"}: ${match.routine.name}\n$message"
            }
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }
}
