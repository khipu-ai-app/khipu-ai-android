package pe.khipuai.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pe.khipuai.app.data.local.entity.ReviewEntity

@Dao
interface ReviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: ReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reviews: List<ReviewEntity>)

    @Query("SELECT * FROM reviews ORDER BY next_review_at ASC")
    fun observeAll(): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE next_review_at <= :nowMillis ORDER BY next_review_at ASC")
    fun observeDueNow(nowMillis: Long): Flow<List<ReviewEntity>>

    @Query("DELETE FROM reviews WHERE concept_id = :conceptId")
    suspend fun deleteByConcept(conceptId: String)
}
