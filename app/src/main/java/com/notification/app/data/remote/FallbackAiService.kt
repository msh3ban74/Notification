package com.notification.app.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * المزود الاحتياطي — a second brain for Rafeeq. When Gemini's free quota is
 * fully exhausted, the chat falls back to any OpenAI-compatible provider
 * (default: Groq, which has a generous free tier) using a key the user
 * pastes in Settings. Text-only: tool calls (creating reminders/debts) stay
 * on the primary pipeline, and the fallback is told to say so warmly.
 */
data class OpenAiMessage(
    val role: String,       // "system" | "user" | "assistant"
    val content: String
)

data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val max_tokens: Int = 1024
)

data class OpenAiChoice(val message: OpenAiMessage? = null)

data class OpenAiChatResponse(val choices: List<OpenAiChoice>? = null)

interface FallbackAiService {

    @POST("openai/v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Body request: OpenAiChatRequest
    ): OpenAiChatResponse

    companion object {
        // Groq's OpenAI-compatible endpoint + a strong free-tier model.
        const val MODEL = "llama-3.3-70b-versatile"
    }
}

object FallbackAiClient {
    private const val BASE_URL = "https://api.groq.com/"

    val service: FallbackAiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(25, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .callTimeout(30, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(
                MoshiConverterFactory.create(
                    Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                )
            )
            .build()
            .create(FallbackAiService::class.java)
    }
}
