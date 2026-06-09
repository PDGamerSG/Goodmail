package com.example.goodmail.data.remote.groq

import com.example.goodmail.data.remote.groq.models.ChatRequest
import com.example.goodmail.data.remote.groq.models.ChatResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/** Groq is OpenAI-compatible; base URL is https://api.groq.com/. */
interface GroqApiService {

    @POST("openai/v1/chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest,
    ): ChatResponse
}
