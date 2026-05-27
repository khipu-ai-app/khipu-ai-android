package pe.khipuai.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pe.khipuai.app.data.local.entity.UploadQueueEntity

@Dao
interface UploadQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpload(upload: UploadQueueEntity)

    @Query("SELECT * FROM upload_queue ORDER BY timestamp ASC")
    suspend fun getAllPendingUploads(): List<UploadQueueEntity>

    @Delete
    suspend fun deleteUpload(upload: UploadQueueEntity)

    @Query("DELETE FROM upload_queue WHERE id = :id")
    suspend fun deleteById(id: String)
}
