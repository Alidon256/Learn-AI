package com.example.learnai.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnai.BuildConfig
import com.example.learnai.UiState
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Message(
    val content: String,
    val isUser: Boolean,
    val fileUri: Uri? = null
)

sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    data class Transcribed(val text: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

class ChatViewModel(
    private val context: Context
) : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _voiceState: MutableStateFlow<VoiceState> = MutableStateFlow(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _messages: MutableStateFlow<List<Message>> = MutableStateFlow(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-preview", // Note: Verify this model is supported; fallback to "gemini-1.5-pro" if needed
        apiKey = BuildConfig.apiKey
    )

    var selectedFileUri: Uri? by mutableStateOf(null)
        private set

    private val speechRecognizer: SpeechRecognizer by lazy {
        SpeechRecognizer.createSpeechRecognizer(context)
    }

    fun setSelectedFile(uri: Uri) {
        selectedFileUri = uri
    }

    fun sendPrompt(prompt: String, isFileMessage: Boolean = false) {
        // Add user message to chat history
        val userMessage = Message(
            content = if (isFileMessage) prompt else prompt,
            isUser = true,
            fileUri = if (isFileMessage) selectedFileUri else null
        )
        _messages.value = _messages.value + userMessage

        _uiState.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentBlock = content {
                    text(prompt)
                    selectedFileUri?.let { uri ->
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val fileBytes = inputStream?.readBytes()
                        inputStream?.close()

                        if (fileBytes != null) {
                            when {
                                uri.toString().endsWith(".jpg", true) || uri.toString().endsWith(".png", true) -> {
                                    // Convert ByteArray to Bitmap for image input
                                    val bitmap = BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size)
                                    if (bitmap != null) {
                                        image(bitmap)
                                    } else {
                                        text("Failed to decode image: $uri")
                                    }
                                }
                                uri.toString().endsWith(".pdf", true) || uri.toString().endsWith(".txt", true) -> {
                                    // For text files, assume UTF-8 encoding (simplified; use PdfBox for PDFs)
                                    val fileText = fileBytes.toString(Charsets.UTF_8)
                                    text("User uploaded document content: $fileText")
                                }
                                else -> {
                                    text("Unsupported file type: $uri")
                                }
                            }
                        } else {
                            text("Unable to read file: $uri")
                        }
                    }
                }
                val response = generativeModel.generateContent(contentBlock)
                response.text?.let { outputContent ->
                    _uiState.value = UiState.Success(outputContent)
                    _messages.value = _messages.value + Message(content = outputContent, isUser = false)
                } ?: run {
                    _uiState.value = UiState.Error("No response from AI model")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Unknown error")
            } finally {
                selectedFileUri = null // Clear file after sending
            }
        }
    }

    fun toggleVoiceInput(onTranscribed: (String) -> Unit) {
        when (_voiceState.value) {
            is VoiceState.Idle -> {
                _voiceState.value = VoiceState.Listening

                // Set up the SpeechRecognizer listener
                val recognitionListener = object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _voiceState.value = VoiceState.Listening
                    }

                    override fun onBeginningOfSpeech() {
                        // Optional: Can add feedback if needed
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Optional: Can use for visual feedback of audio levels
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {
                        // Not needed for this use case
                    }

                    override fun onEndOfSpeech() {
                        // Optional: Can add feedback if needed
                    }

                    override fun onError(error: Int) {
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                            else -> "Unknown error: $error"
                        }
                        _voiceState.value = VoiceState.Error(errorMessage)
                        speechRecognizer.cancel()
                        speechRecognizer.destroy()
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val transcribedText = matches?.firstOrNull() ?: ""
                        if (transcribedText.isNotEmpty()) {
                            _voiceState.value = VoiceState.Transcribed(transcribedText)
                            onTranscribed(transcribedText)
                        } else {
                            _voiceState.value = VoiceState.Error("No speech recognized")
                        }
                        speechRecognizer.cancel()
                        speechRecognizer.destroy()
                        _voiceState.value = VoiceState.Idle
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        // Optional: Can use for real-time transcription if needed
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {
                        // Not needed for this use case
                    }
                }

                // Set up the SpeechRecognizer
                speechRecognizer.setRecognitionListener(recognitionListener)

                // Create intent for speech recognition
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US") // Adjust language as needed
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                // Start listening
                try {
                    speechRecognizer.startListening(intent)
                } catch (e: Exception) {
                    _voiceState.value = VoiceState.Error("Failed to start speech recognition: ${e.localizedMessage}")
                    speechRecognizer.cancel()
                    speechRecognizer.destroy()
                    _voiceState.value = VoiceState.Idle
                }
            }
            else -> {
                _voiceState.value = VoiceState.Idle
                speechRecognizer.cancel()
                speechRecognizer.destroy()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer.destroy()
    }
}