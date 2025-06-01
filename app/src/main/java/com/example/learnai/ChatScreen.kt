package com.example.learnai

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.learnai.ui.theme.LearnAITheme
import com.example.learnai.viewmodel.ChatViewModel
import com.example.learnai.viewmodel.Message
import com.example.learnai.viewmodel.VoiceState
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel = viewModel()
) {
    val placeholderPrompt = stringResource(R.string.prompt_placeholder)
    var prompt by rememberSaveable { mutableStateOf("") }
    val uiState by chatViewModel.uiState.collectAsState()
    val voiceState by chatViewModel.voiceState.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            chatViewModel.setSelectedFile(uri)
            scope.launch {
                chatViewModel.sendPrompt("Analyze this file: ${uri.lastPathSegment}", isFileMessage = true)
            }
        }
    }

    // Auto-scroll to the latest message
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF5F7FA), Color(0xFFE4E7EB))))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
           Row(
         modifier = Modifier
             .fillMaxWidth()
             .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
         verticalAlignment = Alignment.Top,
         horizontalArrangement = Arrangement.Start
     ) {
         Icon(
             imageVector = Icons.Default.Psychology, // AI-like icon// Replace with an AI icon if available
             contentDescription = "AI Icon",
             tint = MaterialTheme.colorScheme.primary,
             modifier = Modifier.size(32.dp)
         )
         Spacer(modifier = Modifier.width(8.dp))
         Text(
             text = "Learn AI",
             style = MaterialTheme.typography.headlineSmall,
             color = MaterialTheme.colorScheme.primary
         )
     }
            // Chat history
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    ChatMessageBubble(
                        message = message,
                        isUser = message.isUser
                    )
                }

                // Loading indicator
                item {
                    AnimatedVisibility(
                        visible = uiState is UiState.Loading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .wrapContentWidth(Alignment.Start)
                        )
                    }
                }

                // Error message
                item {
                    AnimatedVisibility(
                        visible = uiState is UiState.Error,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = (uiState as? UiState.Error)?.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .wrapContentWidth(Alignment.Start)
                        )
                    }
                }
            }

            // Input section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.White, shape = RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                // Voice input status
                AnimatedVisibility(
                    visible = voiceState is VoiceState.Listening,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Listening...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        placeholder = { Text(placeholderPrompt) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF1F3F5),
                            unfocusedContainerColor = Color(0xFFF1F3F5),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        trailingIcon = {
                            Row {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            chatViewModel.toggleVoiceInput { transcribedText ->
                                                prompt = transcribedText
                                                chatViewModel.sendPrompt(transcribedText)
                                            }
                                        }
                                    },
                                    enabled = voiceState !is VoiceState.Listening
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice Input",
                                        tint = if (voiceState is VoiceState.Listening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { filePickerLauncher.launch("*/*") },
                                    enabled = voiceState !is VoiceState.Listening
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachFile,
                                        contentDescription = "Upload File",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    )

                    IconButton(
                        onClick = {
                            if (prompt.isNotEmpty()) {
                                chatViewModel.sendPrompt(prompt)
                                prompt = ""
                            }
                        },
                        enabled = prompt.isNotEmpty(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (prompt.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Message",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: Message,
    isUser: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .wrapContentWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primary else Color(0xFFF1F3F5)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (message.fileUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "File: ${message.fileUri.lastPathSegment ?: "Unknown"}",
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun ChatScreenPreview() {
    LearnAITheme {
        ChatScreen()
    }
}