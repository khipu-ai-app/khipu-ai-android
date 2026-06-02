package pe.khipuai.app.data.repository

import kotlinx.coroutines.flow.Flow
import pe.khipuai.app.data.local.dao.CourseDao
import pe.khipuai.app.data.local.entity.CourseEntity
import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.CourseCreateRequest
import pe.khipuai.app.data.remote.dto.CourseResponse
import pe.khipuai.app.data.remote.dto.CourseUpdateRequest
import pe.khipuai.app.data.remote.dto.OnboardingRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val apiService: KhipuApiService,
    private val courseDao: CourseDao
) {

    /** Flow reactivo de todos los cursos desde Room (Single Source of Truth). */
    fun observeAll(): Flow<List<CourseEntity>> = courseDao.observeAll()

    /** Obtiene un curso específico por ID desde Room. */
    suspend fun getById(courseId: String): CourseEntity? = courseDao.getById(courseId)

    /**
     * Sincroniza cursos desde la API hacia Room.
     * Retorna también la lista para compatibilidad con código existente.
     */
    suspend fun fetchMyCourses(): Result<List<CourseResponse>> {
        return try {
            val remote = apiService.getMyCourses()
            // Persistir en Room inmediatamente
            val entities = remote.map { dto ->
                CourseEntity(id = dto.id, name = dto.name, color = dto.color)
            }
            courseDao.upsertAll(entities)
            Result.success(remote)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCourse(name: String, color: String): Result<CourseResponse> {
        return try {
            val response = apiService.createCourse(CourseCreateRequest(name = name, color = color))
            courseDao.upsertAll(listOf(CourseEntity(id = response.id, name = response.name, color = response.color)))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCourse(courseId: String, name: String?, color: String?): Result<CourseResponse> {
        return try {
            val response = apiService.updateCourse(courseId, CourseUpdateRequest(name = name, color = color))
            courseDao.upsertAll(listOf(CourseEntity(id = response.id, name = response.name, color = response.color)))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun archiveCourse(courseId: String): Result<Unit> {
        return try {
            apiService.deleteCourse(courseId)
            courseDao.deleteById(courseId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCatalog(profileType: String): Result<List<String>> {
        return try {
            Result.success(apiService.getCatalogByProfile(profileType))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitOnboarding(
        fullName: String,
        profileType: String,
        selectedCourses: List<String>
    ): Result<Unit> {
        return try {
            apiService.completeOnboarding(
                OnboardingRequest(
                    fullName = fullName,
                    profileType = profileType,
                    selectedCatalogCourses = selectedCourses,
                    customCourses = emptyList()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
