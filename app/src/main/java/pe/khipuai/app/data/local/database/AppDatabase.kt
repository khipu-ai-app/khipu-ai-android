package pe.khipuai.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import pe.khipuai.app.data.local.dao.CourseDao
import pe.khipuai.app.data.local.dao.NoteDao
import pe.khipuai.app.data.local.dao.ReviewDao
import pe.khipuai.app.data.local.dao.UploadQueueDao
import pe.khipuai.app.data.local.entity.CourseEntity
import pe.khipuai.app.data.local.entity.NoteEntity
import pe.khipuai.app.data.local.entity.ReviewEntity
import pe.khipuai.app.data.local.entity.UploadQueueEntity

@Database(
    entities = [
        UploadQueueEntity::class,
        CourseEntity::class,
        NoteEntity::class,
        ReviewEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun uploadQueueDao(): UploadQueueDao
    abstract fun courseDao(): CourseDao
    abstract fun noteDao(): NoteDao
    abstract fun reviewDao(): ReviewDao
}
