package pe.khipuai.app.data.remote.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import pe.khipuai.app.data.local.database.AppDatabase
import pe.khipuai.app.data.repository.UploadRepository
import java.io.File

@HiltWorker
class UploadSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val uploadRepository: UploadRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val uploadDao = database.uploadQueueDao()
        val pendingUploads = uploadDao.getAllPendingUploads()

        if (pendingUploads.isEmpty()) {
            return Result.success()
        }

        var allSuccessful = true

        for (upload in pendingUploads) {
            try {
                val file = File(upload.filePath)
                if (!file.exists()) {
                    uploadDao.deleteUpload(upload)
                    continue
                }

                val courseId = upload.courseId
                // Ejecutamos la subida de forma real en segundo plano
                val uploadResult = uploadRepository.uploadFile(file, upload.fileType, courseId)

                if (uploadResult.isSuccess) {
                    uploadDao.deleteUpload(upload)
                } else {
                    allSuccessful = false
                }
            } catch (e: Exception) {
                allSuccessful = false
            }
        }

        return if (allSuccessful) Result.success() else Result.retry()
    }
}
