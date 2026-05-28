package pe.khipuai.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["course_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["course_id"])]
)
data class NoteEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "summary")
    val summary: String,

    @ColumnInfo(name = "course_id")
    val courseId: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "difficulty_level")
    val difficultyLevel: String = "medium",

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)
