package com.example.goodmail.data.remote.groq.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.1,
    @SerialName("response_format") val responseFormat: ResponseFormat? = null,
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
data class ResponseFormat(
    val type: String,
)

@Serializable
data class ChatResponse(
    val choices: List<Choice> = emptyList(),
)

@Serializable
data class Choice(
    val message: ChatMessage,
)
