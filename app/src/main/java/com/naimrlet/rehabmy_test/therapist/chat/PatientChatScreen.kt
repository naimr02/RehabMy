package com.naimrlet.rehabmy_test.therapist.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.naimrlet.rehabmy_test.chat.model.MessageState
import com.naimrlet.rehabmy_test.chat.ui.components.*

/**
 * Patient-specific chat screen for therapists
 * This allows therapists to chat with a specific selected patient
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientChatScreen(
    patientId: String,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PatientChatViewModel = hiltViewModel()
) {
    // Initialize chat with the specific patient
    LaunchedEffect(patientId) {
        viewModel.initializeChatWithPatient(patientId)
    }

    val currentUser by viewModel.currentUser.collectAsState()
    val chatPartner by viewModel.chatPartner.collectAsState()
    val messageState by viewModel.messageState.collectAsState()
    val messageInputState by viewModel.messageInputState.collectAsState()
    val isInitialized by viewModel.isInitialized.collectAsState()

    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messageState) {
        val currentState = messageState // Capture state to enable smart casting
        if (currentState is MessageState.Success && currentState.messages.isNotEmpty()) {
            listState.animateScrollToItem(currentState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        chatPartner?.let { partner ->
                            AsyncImage(
                                model = partner.profileImageUrl.ifEmpty { "https://via.placeholder.com/40" },
                                contentDescription = "Profile image",
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(end = 12.dp)
                            )
                            Column {
                                Text(
                                    text = partner.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (partner.isTherapist) "Physiotherapist" else "Patient",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } ?: run {
                            Text("Chat")
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshMessages() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = messageState) {
                is MessageState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is MessageState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Error loading messages",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = { viewModel.retryChatInitialization(patientId) }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is MessageState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.messages) { message ->
                            MessageBubble(
                                message = message,
                                isFromCurrentUser = message.senderId == currentUser?.id,
                                partnerImageUrl = chatPartner?.profileImageUrl ?: ""
                            )
                        }
                    }

                    // Message input
                    MessageInput(
                        inputState = messageInputState,
                        onTextChange = viewModel::updateMessageText,
                        onSendClick = viewModel::sendMessage,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
