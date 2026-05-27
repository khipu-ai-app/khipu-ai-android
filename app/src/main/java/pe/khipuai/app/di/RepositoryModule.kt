package pe.khipuai.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import kotlinx.serialization.json.Json
import pe.khipuai.app.data.local.TokenManager
import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.repository.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        apiService: KhipuApiService,
        tokenManager: TokenManager
    ): AuthRepository = AuthRepository(apiService, tokenManager)

    @Provides
    @Singleton
    fun provideCourseRepository(apiService: KhipuApiService): CourseRepository =
        CourseRepository(apiService)

    @Provides
    @Singleton
    fun provideGraphRepository(apiService: KhipuApiService): GraphRepository =
        GraphRepository(apiService)

    @Provides
    @Singleton
    fun provideNoteRepository(apiService: KhipuApiService): NoteRepository =
        NoteRepository(apiService)

    @Provides
    @Singleton
    fun providePlannerRepository(apiService: KhipuApiService): PlannerRepository =
        PlannerRepository(apiService)

    @Provides
    @Singleton
    fun provideUploadRepository(apiService: KhipuApiService): UploadRepository =
        UploadRepository(apiService)

    @Provides
    @Singleton
    fun provideTutorRepository(
        apiService: KhipuApiService,
        okHttpClient: OkHttpClient,
        json: Json
    ): TutorRepository =
        TutorRepository(apiService, okHttpClient, json)
}
