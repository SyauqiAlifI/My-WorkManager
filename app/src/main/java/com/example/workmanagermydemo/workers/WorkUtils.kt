package com.example.workmanagermydemo.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.workmanagermydemo.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

fun makeStatusNotification(message: String, context: Context) {

    // Membuat channel jika diperlukan
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        /* Membuat NotificationChannel */
        val name = VERBOSE_NOTIFICATION_CHANNEL_NAME
        val description = VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance)

        channel.description = description

        // Tambah channel ke dalam sistem
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        notificationManager?.createNotificationChannel(channel)
    }

    // Membuat Notifikasi
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(NOTIFICATION_TITLE)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVibrate(LongArray(0))

    // Menampilkan Notifikasi
    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
}

private const val TAG = "WorkerUtils"

fun sleep() {

    try {
        Thread.sleep(DELAY_TIME_MILLIS, 0)
    } catch (e: InterruptedException) {
        Log.e(TAG, e.message.toString())
    }

}

@WorkerThread
fun blurBitmap(bitmap: Bitmap, applicationContext: Context): Bitmap {

    lateinit var rsContext: RenderScript

    try {

        // Output Bitmap
        val output = Bitmap.createBitmap(
            bitmap.width, bitmap.height, bitmap.config)

        // Blur image
        rsContext = RenderScript.create(applicationContext, RenderScript.ContextType.DEBUG)

        // Input Allocation
        val inAlloc = Allocation.createFromBitmap(rsContext, bitmap)

        // Output Allocation
        val outAlloc = Allocation.createTyped(rsContext, inAlloc.type)

        val theIntrinsic =
            ScriptIntrinsicBlur.create(rsContext, Element.U8_4(rsContext))

        theIntrinsic.apply {
            setRadius(10f)
            theIntrinsic.setInput(inAlloc)
            theIntrinsic.forEach(outAlloc)
        }

        outAlloc.copyTo(output)

        return output

    } finally {
        rsContext.finish()
    }
}

@Throws(FileNotFoundException::class)
fun writeBitmapToFile(applicationContext: Context, bitmap: Bitmap): Uri {

    val name = String.format("blur-filter-output-%s.png", UUID.randomUUID().toString())
    val outputDir = File(applicationContext.filesDir, OUTPUT_PATH)

    if (!outputDir.exists()) {
        outputDir.mkdirs() // should succeed
    }

    val outputFile = File(outputDir, name)
    var out: FileOutputStream? = null

    try {

        out = FileOutputStream(outputFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /* ignored for PNG */, out)

    } finally {

        out?.let {
            try {
                it.close()
            } catch (ignore: IOException) {
            }

        }

    }

    return Uri.fromFile(outputFile)
}