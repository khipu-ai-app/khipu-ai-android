package pe.khipuai.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pe.khipuai.app.BuildConfig
import pe.khipuai.app.core.datastore.SessionDataStore
import pe.khipuai.app.core.network.TokenAuthenticator
import pe.khipuai.app.data.remote.KhipuApiService
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(sessionDataStore: SessionDataStore): Interceptor = Interceptor { chain ->
        val original = chain.request()
        val path = original.url.encodedPath

        // No adjuntar el token guardado en endpoints de autenticación pública.
        // Si lo hiciéramos, un POST /auth/login con credenciales malas
        // (HTTP 401) activaría el TokenAuthenticator → refresh con token
        // expirado → 401 → emitir SessionExpired → navegar a Login destruyendo
        // el formulario y haciendo desaparecer el mensaje de error.
        if (path.endsWith("/auth/login") ||
            path.endsWith("/auth/register") ||
            path.endsWith("/auth/refresh") ||
            path.endsWith("/auth/google")
        ) {
            return@Interceptor chain.proceed(original)
        }

        val token = runBlocking { sessionDataStore.tokenFlow.first() }
        val requestBuilder = original.newBuilder()

        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return@Interceptor chain.proceed(requestBuilder.build())
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .build()
    }

    @Provides
    @Singleton
    fun provideKhipuApiService(okHttpClient: OkHttpClient, json: Json): KhipuApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(KhipuApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSessionDataStore(@ApplicationContext context: Context): SessionDataStore =
        SessionDataStore(context)
}