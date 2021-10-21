package io.moyuru.directorydownloadexternalstoragepermission

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import io.moyuru.directorydownloadexternalstoragepermission.databinding.ActivityMainBinding
import permissions.dispatcher.ktx.constructPermissionsRequest
import java.io.File

class MainActivity : AppCompatActivity() {
  companion object {
    const val FILE_URL = "https://github.com/MoyuruAizawa/Images/raw/master/TimetableLayout/sample_01.gif"
  }

  private lateinit var binding: ActivityMainBinding
  private val downloadManager by lazy { requireNotNull(getSystemService<DownloadManager>()) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    binding.buttonMainDownload.setOnClickListener { download() }
    binding.buttonMainClear.setOnClickListener { clear() }
  }

  private fun clear() {
    val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
    val sampleDir = dir.listFiles()?.firstOrNull { it.name == "sample" }
    Log.i("CLEAR", "CLEAR ${sampleDir?.listFiles()?.map { it.name }?.toString()}")
    dir.deleteRecursively()
  }

  private fun download() {
    val request = DownloadManager.Request(Uri.parse(FILE_URL))
      .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
      .setDestinationInExternalFilesDir(
        this,
        Environment.DIRECTORY_DOWNLOADS,
        "/sample/${System.currentTimeMillis()}.gif"
      )
    val downloadId = downloadManager.enqueue(request)
    Log.i("DOWNLOAD", "Download $downloadId")
    dontCareThisBlock(downloadId)
  }

  private fun dontCareThisBlock(downloadId: Long) {
    registerReceiver(object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        val intentDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (intentDownloadId != downloadId) return

        downloadManager.query(DownloadManager.Query().setFilterById(intentDownloadId)).use { cursor ->
          if (!cursor.moveToFirst()) Log.i("DOWNLOAD", "Cursor error")
          val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
          if (status == DownloadManager.STATUS_SUCCESSFUL) {
            val uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
            val file = Uri.parse(uri).path?.let(::File)
            Log.i("DOWNLOAD", "Downloaded $intentDownloadId, ${file?.path}")
          } else {
            Log.i("DOWNLOAD", "Download failed $intentDownloadId")
          }
        }
      }
    }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
  }

}