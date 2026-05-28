package pe.khipuai.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey
    @ColumnInfo(name = "concept_id")
    val conceptId: String,

    @ColumnInfo(name = "concept_name")
    val conceptName: String,

    @ColumnInfo(name = "course_id")
    val courseId: String?,

    @ColumnInfo(name = "interval_days")
    val intervalDays: Int = 1,

    @ColumnInfo(name = "next_review_at")
    val nextReviewAt: Long,

    @ColumnInfo(name = "ease_factor")
    val easeFactor: Float = 2.5f,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)
