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
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val tools: List<GeminiTool>? = null,
    val systemInstruction: GeminiContent? = null
)

data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String? = null,
    val functionCall: GeminiFunctionCall? = null,
    val functionResponse: GeminiFunctionResponse? = null,
    // Newer Gemini models attach an opaque "thought signature" to a
    // function-call part and REJECT the follow-up turn if it isn't echoed
    // back verbatim. Round-tripping it keeps tool calls working.
    val thoughtSignature: String? = null
)

data class GeminiFunctionCall(
    val name: String,
    val args: Map<String, Any?> = emptyMap(),
    val id: String? = null
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
    // Pinned model names keep dying (1.5-flash retired; 2.5-flash closed
    // to new API keys — both verified via the AI Smoke Test workflow), so
    // the model is a parameter around Google's rolling aliases:
    //  • PRIMARY_MODEL  — newest flash (best quality for chat)
    //  • LITE_MODEL     — flash-lite: a SEPARATE, much larger free-tier
    //    quota bucket. Used for dashboard suggestions and as the automatic
    //    fallback when the primary model answers 429 (rate limit), so the
    //    assistant answers instead of showing "the service is busy".
    // No generationConfig: newer models reject the old thinkingBudget
    // knob (400 INVALID_ARGUMENT).
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    companion object {
        const val PRIMARY_MODEL = "gemini-flash-latest"
        const val LITE_MODEL = "gemini-flash-lite-latest"
    }
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
                // Even in debug, never print the key-bearing URL param or the
                // Authorization header, and stay at BASIC so user data (names,
                // debts, reminders) doesn't land in logcat.
                redactQueryParams("key")
                redactHeader("Authorization")
                level = if (com.notification.app.BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
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
