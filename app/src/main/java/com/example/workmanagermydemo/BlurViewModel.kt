package com.example.workmanagermydemo

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.example.workmanagermydemo.workers.BlurWorker
import com.example.workmanagermydemo.workers.CleanupWorker
import com.example.workmanagermydemo.workers.SaveImageToFileWorker

class BlurViewModel(application: Application) : ViewModel() {

    private var imageUri: Uri? = null
    internal var outputUri: Uri? = null
    private val workManager = WorkManager.getInstance(application)
    internal val outputWorkInfos: LiveData<List<WorkInfo>> = workManager.getWorkInfosByTagLiveData(TAG_OUTPUT)

    init {

        // Memastikan Current id berubah UI mengikuti perubahan
        imageUri = getImageUri(application.applicationContext)

    }

    internal fun cancelWork() {

        workManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME)

    }

    private fun createInputDataForUri(): Data {

        val builder = Data.Builder()

        imageUri?.let {
            builder.putString(KEY_IMAGE_URI, imageUri.toString())
        }

        return builder.build()

    }

    internal fun applyBlur(blurLevel: Int) {

        // Menambah WorkRequest untuk menghapus file yang sementara
        var continuation = workManager
            .beginUniqueWork(
                IMAGE_MANIPULATION_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.from(CleanupWorker::class.java)
            )

        // WorkRequest untuk blur image
        for (i in 0 until blurLevel) {

            val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()

            // Masukkan Uri Jika ini adalah operasi yang pertama
            if (i == 0) {
                blurBuilder.setInputData(createInputDataForUri())
            }

            continuation = continuation.then(blurBuilder.build())
        }

        // Charging Constraint
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .build()

        // WorkRequest save image ke filesystem
        val save = OneTimeWorkRequestBuilder<SaveImageToFileWorker>()
            .setConstraints(constraints)
            .addTag(TAG_OUTPUT)
            .build()

        continuation = continuation.then(save)

        // Memulai work
        continuation.enqueue()
    }

    private fun uriOrNull(uriString: String?): Uri? {

        return if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            null
        }

    }

    private fun getImageUri(context: Context): Uri {
        val resources = context.resources

        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceTypeName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceEntryName(R.drawable.android_cupcake))
            .build()
    }

    internal fun setOutputUri(outputImageUri: String?) {
        outputUri = uriOrNull(outputImageUri)
    }
}

class BlurViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(BlurViewModel::class.java)) {
            BlurViewModel(application) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
