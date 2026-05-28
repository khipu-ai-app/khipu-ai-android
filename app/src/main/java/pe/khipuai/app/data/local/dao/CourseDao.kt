package pe.khipuai.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pe.khipuai.app.data.local.entity.CourseEntity

@Dao
interface CourseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(courses: List<CourseEntity>)

    @Query("SELECT * FROM courses ORDER BY name ASC")
    fun observeAll(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CourseEntity?

    @Query("DELETE FROM courses")
    suspend fun deleteAll()
}
