package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileOutputStream

private const val TAG = "AiVideoLabService"

enum class AiImageStyle(val label: String, val promptSuffix: String) {
    CINEMATIC_8K("Cinematic 8K", "cinematic 8K film still, anamorphic lens, dramatic volumetric lighting, ultra sharp"),
    ANIME("Anime", "high-budget studio anime art style, vibrant cel shading, atmospheric light rays"),
    CYBERPUNK("Cyberpunk", "futuristic cyberpunk neon city aesthetic, electric purple and cyan rim lighting, rain reflections"),
    PIXAR_3D("3D Pixar", "3D animated movie render, soft global illumination, expressive stylized textures, Pixar quality"),
    REALISTIC("Realistic", "photorealistic DSLR photography, 50mm f/1.4 shallow depth of field, natural color"),
    SPIRITUAL("Spiritual", "divine golden radiant aura, peaceful spiritual devotional art, luminous temple atmosphere"),
    RETRO_90S("Retro 90s", "1990s analog Kodak film photography, warm halation, authentic film grain")
}

enum class AiCameraMotion(val label: String) {
    SLOW_ZOOM("Slow Zoom"),
    DRONE_PAN("Drone Pan"),
    PARALLAX_3D("Parallax 3D"),
    PARTICLE_FLOW("Particle Flow")
}

@JsonClass(generateAdapter = true)
data class AiScriptScene(
    val title: String = "",
    val caption: String = "",
    val visualPrompt: String = "",
    val durationSec: Int = 3
)

@JsonClass(generateAdapter = true)
data class AiScriptPlan(
    val projectTitle: String = "AI Story Reel",
    val scenes: List<AiScriptScene> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AiLyricsPlan(
    val songTitle: String = "Neon Pulse",
    val lines: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AiPromoScene(
    val headline: String = "",
    val subtext: String = "",
    val durationSec: Int = 3
)

@JsonClass(generateAdapter = true)
data class AiPromoPlan(
    val campaignTitle: String = "Promo Ad",
    val scenes: List<AiPromoScene> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ViralKitResult(
    val youtubeTitles: List<String> = emptyList(),
    val captions: List<String> = emptyList(),
    val hashtags: List<String> = emptyList()
)

sealed interface AiTaskState<out T> {
    data object Idle : AiTaskState<Nothing>
    data class Loading(val status: String) : AiTaskState<Nothing>
    data class Success<T>(val data: T) : AiTaskState<T>
    data class Error(val message: String) : AiTaskState<Nothing>
}

class AiVideoLabService(private val context: Context) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Lazy models initialized after App Check has run in MainActivity.onCreate()
    private val imageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    private val scriptPlanSchema by lazy {
        val sceneSchema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string("Short scene title (2-4 words)"),
                "caption" to Schema.string("Punchy on-screen subtitle line in the requested language"),
                "visualPrompt" to Schema.string("Vivid visual scene description in English"),
                "durationSec" to Schema.integer("Scene length between 2 and 5 seconds")
            )
        )
        Schema.obj(
            properties = mapOf(
                "projectTitle" to Schema.string("Short catchy video title"),
                "scenes" to Schema.array(sceneSchema, "4 to 5 visual storyboard scenes")
            )
        )
    }

    private val scriptModelStandard by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = scriptPlanSchema
            }
        )
    }

    // Ultra-fast lightweight Gemini 3 Flash-Lite for weak / low-bandwidth cellular networks
    private val scriptModelLite by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = scriptPlanSchema
            }
        )
    }

    private val lyricsSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "songTitle" to Schema.string("Short song title"),
                "lines" to Schema.array(Schema.string("One rhythmic lyric line"), "6 to 8 short lines")
            )
        )
    }

    private val lyricsModelStandard by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = lyricsSchema
            }
        )
    }

    private val lyricsModelLite by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = lyricsSchema
            }
        )
    }

    private val promoSchema by lazy {
        val promoSceneSchema = Schema.obj(
            properties = mapOf(
                "headline" to Schema.string("Bold kinetic headline (2-5 words)"),
                "subtext" to Schema.string("Supporting caption or call-to-action"),
                "durationSec" to Schema.integer("Duration 2 to 4 seconds")
            )
        )
        Schema.obj(
            properties = mapOf(
                "campaignTitle" to Schema.string("Short ad campaign name"),
                "scenes" to Schema.array(promoSceneSchema, "4 to 5 fast-paced ad scenes")
            )
        )
    }

    private val promoModelStandard by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = promoSchema
            }
        )
    }

    private val promoModelLite by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = promoSchema
            }
        )
    }

    private val viralSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "youtubeTitles" to Schema.array(Schema.string("High-CTR YouTube / Shorts title"), "3 catchy titles"),
                "captions" to Schema.array(Schema.string("Engaging Instagram / Facebook reel caption with hook and CTA"), "2 captions"),
                "hashtags" to Schema.array(Schema.string("Single trending hashtag starting with #"), "12 relevant hashtags")
            )
        )
    }

    private val viralKitModelStandard by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = viralSchema
            }
        )
    }

    private val viralKitModelLite by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = viralSchema
            }
        )
    }

    /**
     * Resilient weak-network runner: retries transient timeouts or unstable-signal IO errors
     * with exponential backoff while preserving coroutine cancellation and surfacing quota errors immediately.
     */
    private suspend fun <T> executeWithWeakNetworkRetry(
        maxAttempts: Int,
        timeoutMs: Long,
        onRetryStatus: ((attempt: Int, total: Int) -> Unit)? = null,
        block: suspend () -> T
    ): T {
        var lastError: Exception? = null
        for (attempt in 1..maxAttempts) {
            try {
                return withTimeout(timeoutMs) { block() }
            } catch (e: CancellationException) {
                // Separate TimeoutCancellationException (subclass of CancellationException) from user cancellation
                if (e is TimeoutCancellationException) {
                    lastError = e
                    Log.w(TAG, "AI request timed out on attempt $attempt/$maxAttempts", e)
                } else {
                    throw e
                }
            } catch (e: QuotaExceededException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "AI request error on attempt $attempt/$maxAttempts: ${e.localizedMessage}", e)
            }

            if (attempt < maxAttempts) {
                onRetryStatus?.invoke(attempt + 1, maxAttempts)
                delay(1200L * attempt)
            }
        }
        throw lastError ?: IllegalStateException("Network request failed after $maxAttempts attempts.")
    }

    suspend fun generateAiImageToLocalFile(
        prompt: String,
        style: AiImageStyle,
        aspectLabel: String,
        lowDataMode: Boolean = false,
        onRetryStatus: ((String) -> Unit)? = null
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val fullPrompt = buildString {
                append(prompt.trim().take(360))
                append(". Style: ")
                append(style.promptSuffix)
                append(". Composed cleanly for ")
                append(aspectLabel)
                append(" framing. No text or watermarks.")
            }
            val bitmap: Bitmap = executeWithWeakNetworkRetry(
                maxAttempts = if (lowDataMode) 3 else 2,
                timeoutMs = 60_000L,
                onRetryStatus = { next, total ->
                    onRetryStatus?.invoke("Weak signal — retrying AI image ($next/$total)…")
                }
            ) {
                val response = imageModel.generateContent(fullPrompt)
                val part = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                part?.image ?: throw IllegalStateException("The AI model did not return an image. Try refining your prompt.")
            }
            val uri = saveBitmapLocally(bitmap, "ai_img", useCompactJpeg = lowDataMode)
            Result.success(uri)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            Result.failure(Exception("Slow network timeout (60s). Enable Lite Mode or retry when signal improves."))
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Quota exceeded", e)
            Result.failure(Exception("AI rate limit reached. Please try again in a moment."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "AI image generation failed. Check network connection."))
        }
    }

    suspend fun generateScriptToVideoPlan(
        scriptOrTopic: String,
        language: String,
        lowDataMode: Boolean = false,
        onRetryStatus: ((String) -> Unit)? = null
    ): Result<AiScriptPlan> = withContext(Dispatchers.IO) {
        try {
            val sceneTarget = if (lowDataMode) "4 concise" else "4 to 5 concise"
            val prompt = """
                You are a viral Reels and YouTube Shorts video director.
                Turn the following topic or script into $sceneTarget, high-retention scenes.
                Language for captions: $language (if Hindi, write natural Devanagari Hindi script).
                Topic / Script:
                ${scriptOrTopic.trim().take(600)}
            """.trimIndent()

            val activeModel = if (lowDataMode) scriptModelLite else scriptModelStandard
            val json = executeWithWeakNetworkRetry(
                maxAttempts = if (lowDataMode) 3 else 2,
                timeoutMs = if (lowDataMode) 55_000L else 45_000L,
                onRetryStatus = { next, total ->
                    onRetryStatus?.invoke("Low network — retrying script ($next/$total)…")
                }
            ) {
                activeModel.generateContent(prompt).text
                    ?: throw IllegalStateException("Empty script response from Gemini.")
            }
            val parsed = moshi.adapter(AiScriptPlan::class.java).fromJson(json)
                ?: throw IllegalStateException("Could not parse structured script plan.")
            if (parsed.scenes.isEmpty()) {
                throw IllegalStateException("No scenes were generated. Please provide more detail.")
            }
            Result.success(parsed)
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Slow network timeout. Try again in Lite Mode or with a shorter prompt."))
        } catch (e: QuotaExceededException) {
            Result.failure(Exception("AI rate limit reached. Please try again shortly."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Script-to-Video failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Script generation failed."))
        }
    }

    suspend fun generateMusicLyrics(
        themeOrMood: String,
        language: String,
        lowDataMode: Boolean = false,
        onRetryStatus: ((String) -> Unit)? = null
    ): Result<AiLyricsPlan> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Write 6 short, rhythmic karaoke lyric lines for a music visualizer Reel.
                Language: $language (use Devanagari if Hindi).
                Theme / Vibe: ${themeOrMood.trim().take(300)}
            """.trimIndent()

            val activeModel = if (lowDataMode) lyricsModelLite else lyricsModelStandard
            val json = executeWithWeakNetworkRetry(
                maxAttempts = if (lowDataMode) 3 else 2,
                timeoutMs = if (lowDataMode) 55_000L else 45_000L,
                onRetryStatus = { next, total ->
                    onRetryStatus?.invoke("Low network — retrying lyrics ($next/$total)…")
                }
            ) {
                activeModel.generateContent(prompt).text
                    ?: throw IllegalStateException("Empty lyrics response from Gemini.")
            }
            val parsed = moshi.adapter(AiLyricsPlan::class.java).fromJson(json)
                ?: throw IllegalStateException("Could not parse lyrics JSON.")
            Result.success(parsed)
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Slow network timeout. Tip: You can also paste lyrics offline anytime!"))
        } catch (e: QuotaExceededException) {
            Result.failure(Exception("AI rate limit reached. Please try again shortly."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Lyrics generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Lyrics generation failed."))
        }
    }

    suspend fun generatePromoOrTalkingPlan(
        productOrTopic: String,
        modeLabel: String,
        language: String,
        lowDataMode: Boolean = false,
        onRetryStatus: ((String) -> Unit)? = null
    ): Result<AiPromoPlan> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Create a punchy 15-second $modeLabel script broken into 4 to 5 timed subtitle scenes.
                Language: $language (use Devanagari if Hindi).
                Brief: ${productOrTopic.trim().take(500)}
            """.trimIndent()

            val activeModel = if (lowDataMode) promoModelLite else promoModelStandard
            val json = executeWithWeakNetworkRetry(
                maxAttempts = if (lowDataMode) 3 else 2,
                timeoutMs = if (lowDataMode) 55_000L else 45_000L,
                onRetryStatus = { next, total ->
                    onRetryStatus?.invoke("Low network — retrying promo script ($next/$total)…")
                }
            ) {
                activeModel.generateContent(prompt).text
                    ?: throw IllegalStateException("Empty promo response from Gemini.")
            }
            val parsed = moshi.adapter(AiPromoPlan::class.java).fromJson(json)
                ?: throw IllegalStateException("Could not parse promo scenes.")
            Result.success(parsed)
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Slow network timeout while generating promo script. Please retry."))
        } catch (e: QuotaExceededException) {
            Result.failure(Exception("AI rate limit reached. Please try again shortly."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Promo generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Promo script generation failed."))
        }
    }

    suspend fun generateViralKit(
        projectTitle: String,
        aspectLabel: String,
        captionTexts: List<String>,
        language: String,
        lowDataMode: Boolean = false,
        onRetryStatus: ((String) -> Unit)? = null
    ): Result<ViralKitResult> = withContext(Dispatchers.IO) {
        try {
            val summary = captionTexts.take(8).joinToString(" | ").ifBlank { projectTitle }
            val prompt = """
                You are a viral social media strategist for YouTube Shorts, Instagram Reels, and Facebook Reels.
                Generate a complete Viral Publishing Kit for this video project:
                - Project Title: $projectTitle
                - Aspect Ratio: $aspectLabel
                - Captions / Context: ${summary.take(400)}
                - Preferred Language: $language
                Return 3 high-CTR YouTube titles, 2 engaging Instagram/Facebook captions, and 12 trending hashtags.
            """.trimIndent()

            val activeModel = if (lowDataMode) viralKitModelLite else viralKitModelStandard
            val json = executeWithWeakNetworkRetry(
                maxAttempts = if (lowDataMode) 3 else 2,
                timeoutMs = if (lowDataMode) 55_000L else 45_000L,
                onRetryStatus = { next, total ->
                    onRetryStatus?.invoke("Weak signal — retrying Viral Kit ($next/$total)…")
                }
            ) {
                activeModel.generateContent(prompt).text
                    ?: throw IllegalStateException("Empty Viral Kit response from Gemini.")
            }
            val parsed = moshi.adapter(ViralKitResult::class.java).fromJson(json)
                ?: throw IllegalStateException("Could not parse Viral Kit JSON.")
            Result.success(parsed)
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Slow network timeout. Tap Retry Viral Kit."))
        } catch (e: QuotaExceededException) {
            Result.failure(Exception("AI rate limit reached. Try again shortly."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Viral Kit generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Viral Kit generation failed."))
        }
    }

    private fun saveBitmapLocally(bitmap: Bitmap, prefix: String, useCompactJpeg: Boolean): Uri {
        val dir = File(context.filesDir, "ai_media").apply { mkdirs() }
        val ext = if (useCompactJpeg) "jpg" else "png"
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.$ext")
        FileOutputStream(file).use { out ->
            if (useCompactJpeg) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 84, out)
            } else {
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
        }
        return Uri.fromFile(file)
    }
}
