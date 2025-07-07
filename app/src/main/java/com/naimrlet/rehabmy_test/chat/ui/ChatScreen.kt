package com.naimrlet.rehabmy_test.chat.ui

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
import com.naimrlet.rehabmy_test.chat.viewmodel.ChatViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBackClick: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
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
            // Messages area
            Box(
                modifier = Modifier.weight(1f)
            ) {
                val currentState = messageState // Capture state to enable smart casting
                when {
                    !isInitialized -> {
                        // Loading state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    currentState is MessageState.Error -> {
                        // Error state
                        ErrorState(
                            message = currentState.message,
                            onRetry = { viewModel.retryChatInitialization() }
                        )
                    }

                    chatPartner == null -> {
                        // No chat partner state
                        currentUser?.let { user ->
                            NoChatPartnerState(isTherapist = user.isTherapist)
                        }
                    }

                    currentState is MessageState.Success -> {
                        if (currentState.messages.isEmpty()) {
                            // Empty state
                            EmptyState(partnerName = chatPartner?.name ?: "")
                        } else {
                            // Messages list
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = currentState.messages,
                                    key = { message -> message.id }
                                ) { message ->
                                    val isFromCurrentUser = message.senderId == currentUser?.id

                                    // Show date header if needed
                                    val messageIndex = currentState.messages.indexOf(message)
                                    if (shouldShowDateHeader(messageIndex, currentState.messages)) {
                                        message.timestamp?.let { timestamp ->
                                            DateHeader(date = timestamp.toDate())
                                        }
                                    }

                                    MessageBubble(
                                        message = message,
                                        isFromCurrentUser = isFromCurrentUser,
                                        partnerImageUrl = chatPartner?.profileImageUrl ?: ""
                                    )
                                }
                            }
                        }
                    }

                    currentState is MessageState.Loading -> {
                        // Loading messages
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }

            // Message input (only show if we have a chat partner)
            if (chatPartner != null) {
                MessageInput(
                    inputState = messageInputState,
                    onTextChange = { viewModel.updateMessageText(it) },
                    onSendClick = { viewModel.sendMessage() }
                )
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

private fun shouldShowDateHeader(
    messageIndex: Int,
    messages: List<com.naimrlet.rehabmy_test.chat.model.ChatMessage>
): Boolean {
    if (messageIndex == 0) return true

    val currentMessage = messages[messageIndex]
    val previousMessage = messages[messageIndex - 1]

    val currentDate = currentMessage.timestamp?.toDate() ?: return false
    val previousDate = previousMessage.timestamp?.toDate() ?: return true

    val currentCal = Calendar.getInstance().apply { time = currentDate }
    val previousCal = Calendar.getInstance().apply { time = previousDate }

    return currentCal.get(Calendar.DAY_OF_YEAR) != previousCal.get(Calendar.DAY_OF_YEAR) ||
            currentCal.get(Calendar.YEAR) != previousCal.get(Calendar.YEAR)
}
