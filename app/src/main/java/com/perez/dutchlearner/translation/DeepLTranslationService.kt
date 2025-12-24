package com.perez.dutchlearner.translation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import com.google.gson.annotations.SerializedName
import java.util.concurrent.TimeUnit

// Respuesta de DeepL API
data class DeepLResponse(
    @SerializedName("translations")
    val translations: List<Translation>
)

data class Translation(
    @SerializedName("detected_source_language")
    val detectedSourceLanguage: String,
    @SerializedName("text")
    val text: String
)

// Interfaz Retrofit para DeepL
interface DeepLApi {
    @POST("v2/translate")
    @FormUrlEncoded
    suspend fun translate(
        @Header("Authorization") authKey: String,
        @Field("text") text: String,
        @Field("target_lang") targetLang: String,
        @Field("source_lang") sourceLang: String? = null
    ): DeepLResponse
}

// Servicio de traducción con cache local
class DeepLTranslationService(private val context: Context) {

    // IMPORTANTE: Reemplaza esto con tu API key de DeepL
    // Para producción, guarda esto en local.properties o BuildConfig
    private val apiKey: String by lazy {
        // Leer desde archivo de properties
        val properties = Properties()
        val secretsFile = File(context.filesDir.parent, "../secrets.properties")

        if (secretsFile.exists()) {
            properties.load(FileInputStream(secretsFile))
            properties.getProperty("DEEPL_API_KEY", "")
        } else {
            // Fallback si el archivo no existe
            ""
        }
    }

    private val api: DeepLApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api-free.deepl.com/") // Free API endpoint
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(DeepLApi::class.java)
    }

    // SharedPreferences para cache local
    private val cache by lazy {
        context.getSharedPreferences("translation_cache", Context.MODE_PRIVATE)
    }

    /**
     * Traduce texto de español a holandés
     * Primero busca en cache local, luego en API
     */
    suspend fun translateToNL(spanishText: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Buscar en cache local
            val cacheKey = "es_nl_${spanishText.hashCode()}"
            val cachedTranslation = cache.getString(cacheKey, null)

            if (cachedTranslation != null) {
                return@withContext Result.success(cachedTranslation)
            }

            // 2. Llamar a DeepL API
            val response = api.translate(
                authKey = "DeepL-Auth-Key $apiKey",
                text = spanishText,
                targetLang = "NL",
                sourceLang = "ES"
            )

            val translatedText = response.translations.firstOrNull()?.text
                ?: return@withContext Result.failure(Exception("No translation received"))

            // 3. Guardar en cache
            cache.edit().putString(cacheKey, translatedText).apply()

            Result.success(translatedText)

        } catch (e: Exception) {
            // Si falla (sin internet, límite excedido, etc.)
            Result.failure(e)
        }
    }

    /**
     * Limpia el cache de traducciones
     */
    fun clearCache() {
        cache.edit().clear().apply()
    }

    /**
     * Obtiene el tamaño del cache
     */
    fun getCacheSize(): Int {
        return cache.all.size
    }

    /**
     * Verifica si hay conexión a internet
     */
    private fun hasInternetConnection(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities != null && (
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                    )
        } catch (e: Exception) {
            false
        }
    }
}

// Clase para obtener instancia del servicio (Singleton)
object TranslationServiceProvider {
    @Volatile
    private var instance: DeepLTranslationService? = null

    fun getInstance(context: Context): DeepLTranslationService {
        return instance ?: synchronized(this) {
            instance ?: DeepLTranslationService(context.applicationContext).also {
                instance = it
            }
        }
    }
}