package pe.khipuai.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.repository.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(apiService: KhipuApiService): AuthRepository =
        AuthRepository(apiService)

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
}
