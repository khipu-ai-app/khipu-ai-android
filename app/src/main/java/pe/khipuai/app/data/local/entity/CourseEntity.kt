package pe.khipuai.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "color")
    val color: String,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "is_from_catalog")
    val isFromCatalog: Boolean = false,

    @ColumnInfo(name = "catalog_key")
    val catalogKey: String? = null,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)

