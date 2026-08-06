package com.notification.app.data.remote

import com.notification.app.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val tools: List<GeminiTool>? = null,
    val systemInstruction: GeminiContent? = null,
    // gemini-2.5-flash "thinks" before answering by default; a chat app
    // with a 15s ceiling wants instant answers, so thinking is off.
    val generationConfig: GeminiGenerationConfig? = GeminiGenerationConfig()
)

data class GeminiGenerationConfig(
    val thinkingConfig: GeminiThinkingConfig? = GeminiThinkingConfig()
)

data class GeminiThinkingConfig(
    val thinkingBudget: Int = 0
)

data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String? = null,
    val functionCall: GeminiFunctionCall? = null,
    val functionResponse: GeminiFunctionResponse? = null
)

data class GeminiFunctionCall(
    val name: String,
    val args: Map<String, Any?>
)

data class GeminiFunctionResponse(
    val name: String,
    val response: Map<String, Any?>
)

data class GeminiTool(
    val functionDeclarations: List<GeminiFunctionDeclaration>
)

data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null
)

interface GeminiApiService {
    // gemini-1.5-flash was retired by Google (every call now returns 404,
    // which made the assistant "never reply"). gemini-2.5-flash is the
    // current stable GA model on the same v1beta generateContent API.
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    // Stability sprint — the old 60s×3 timeouts let a bad connection hang a
    // "frozen" conversation for minutes. Tight budgets fail fast; the
    // ViewModel adds its own 15s generation ceiling with a friendly retry.
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        // SECURITY (v1.0): BODY logging printed full AI payloads and the
        // key-bearing request URL to logcat on user devices. Verbose only
        // in debug builds; silent in release.
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (com.notification.app.BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val geminiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}
