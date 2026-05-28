package pe.khipuai.app.data.remote.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import pe.khipuai.app.data.local.dao.UploadQueueDao
import pe.khipuai.app.data.repository.UploadRepository
import java.io.File

@HiltWorker
class UploadSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val uploadRepository: UploadRepository,
    private val uploadQueueDao: UploadQueueDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pendingUploads = uploadQueueDao.getAllPendingUploads()

        if (pendingUploads.isEmpty()) {
            return Result.success()
        }

        var allSuccessful = true

        for (upload in pendingUploads) {
            try {
                val file = File(upload.filePath)
                if (!file.exists()) {
                    uploadQueueDao.deleteUpload(upload)
                    continue
                }

                val courseId = upload.courseId
                val uploadResult = uploadRepository.uploadFile(file, upload.fileType, courseId)

                if (uploadResult.isSuccess) {
                    uploadQueueDao.deleteUpload(upload)
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
