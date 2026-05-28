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
        val token = runBlocking { sessionDataStore.tokenFlow.first() }
        val requestBuilder = chain.request().newBuilder()

        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        
        val response = chain.proceed(requestBuilder.build())
        
        // Si recibimos 401 (ej. Wipe de BD o token expirado), limpiamos la sesión local
        if (response.code == 401) {
            runBlocking { sessionDataStore.clearToken() }
        }
        
        return@Interceptor response
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
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