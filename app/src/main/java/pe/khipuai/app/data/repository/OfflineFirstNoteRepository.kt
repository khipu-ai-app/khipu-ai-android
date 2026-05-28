package pe.khipuai.app.data.repository

import kotlinx.coroutines.flow.Flow
import pe.khipuai.app.data.local.dao.NoteDao
import pe.khipuai.app.data.local.entity.NoteEntity
import pe.khipuai.app.data.remote.KhipuApiService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio offline-first para notas.
 *
 * Patrón:
 *   1. La UI observa un Flow<List<NoteEntity>> desde Room (siempre tiene datos locales).
 *   2. Al inicializar o refrescar, sincroniza con la API y guarda el resultado en Room.
 *   3. Room emite la actualización y la UI se redibuja reactivamente.
 *
 * REGLA: Nunca inventa datos. Si la red falla y Room está vacío, emite lista vacía.
 */
@Singleton
class OfflineFirstNoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val apiService: KhipuApiService
) {

    /** Flow reactivo de todas las notas — la UI lo observa directamente. */
    fun observeAll(): Flow<List<NoteEntity>> = noteDao.observeAll()

    /** Flow reactivo filtrado por curso. */
    fun observeByCourse(courseId: String): Flow<List<NoteEntity>> =
        noteDao.observeByCourse(courseId)

    /**
     * Sincroniza notas desde la API hacia Room.
     * Llama esto en init{} de los ViewModels o al hacer pull-to-refresh.
     */
    suspend fun syncFromNetwork(): Result<Unit> {
        return try {
            val remoteNotes = apiService.getMyNotes()
            val entities = remoteNotes.map { dto ->
                NoteEntity(
                    id = dto.id,
                    title = dto.title,
                    summary = "",           // El resumen viene en NoteDetail, no en la lista
                    courseId = dto.courseId,
                    createdAt = dto.createdAt,
                    difficultyLevel = "medium"
                )
            }
            noteDao.upsertAll(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Obtiene el detalle de una nota desde la API y actualiza Room. */
    suspend fun syncNoteDetail(noteId: String): Result<Unit> {
        return try {
            val detail = apiService.getNoteDetail(noteId)
            val entity = NoteEntity(
                id = detail.id,
                title = detail.title,
                summary = detail.summary,
                courseId = detail.courseId,
                createdAt = detail.createdAt,
                difficultyLevel = detail.difficultyLevel
            )
            noteDao.upsertAll(listOf(entity))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
