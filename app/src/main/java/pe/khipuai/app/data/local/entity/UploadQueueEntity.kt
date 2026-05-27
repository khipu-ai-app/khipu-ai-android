package pe.khipuai.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upload_queue")
data class UploadQueueEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "file_path")
    val filePath: String,
    
    @ColumnInfo(name = "file_type")
    val fileType: String,
    
    @ColumnInfo(name = "course_id")
    val courseId: String?,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
