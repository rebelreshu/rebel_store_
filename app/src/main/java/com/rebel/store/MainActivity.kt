package com.rebel.store

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var appsContainer: LinearLayout
    private lateinit var txtStatus: TextView
    private lateinit var progressBar: ProgressBar
    private var downloadId: Long = 0

    data class AppInfo(val name: String, val url: String)

    // --- USERNAME = rebelstar ---
    private val rebelApps = listOf(
        AppInfo("Rebel IPTV", "https://github.com/rebelstar/rebel-iptv/releases/download/v1.0/app.apk"),
        AppInfo("Rebel Movies", "https://github.com/rebelstar/rebel-movies/releases/download/v1.0/app.apk")
    )

    private val storeApps = listOf(
        AppInfo("Store Player", "https://github.com/rebelstar/store-player/releases/download/v1.0/app.apk"),
        AppInfo("Store Live", "https://github.com/rebelstar/store-live/releases/download/v1.0/app.apk")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        appsContainer = findViewById(R.id.appsContainer)
        txtStatus = findViewById(R.id.txtStatus)
        progressBar = findViewById(R.id.progressBar)

        val btnRebel = findViewById<Button>(R.id.btnRebel)
        val btnStore = findViewById<Button>(R.id.btnStore)

        btnRebel.setOnClickListener { showApps(rebelApps, "REBEL") }
        btnStore.setOnClickListener { showApps(storeApps, "STORE") }

        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun showApps(apps: List<AppInfo>, category: String) {
        appsContainer.removeAllViews()
        txtStatus.text = "$category Apps - Click GET to install"
        for (app in apps) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(10, 15, 10, 15)
            }
            val nameView = TextView(this).apply {
                text = app.name
                setTextColor(resources.getColor(android.R.color.white))
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val btnGet = Button(this).apply {
                text = "GET"
                setOnClickListener { downloadAndInstall(app) }
            }
            row.addView(nameView)
            row.addView(btnGet)
            appsContainer.addView(row)
        }
    }

    private fun downloadAndInstall(app: AppInfo) {
        txtStatus.text = "Downloading ${app.name}..."
        progressBar.visibility = View.VISIBLE
        try {
            val fileName = "${app.name.replace(" ", "_")}.apk"
            val request = DownloadManager.Request(Uri.parse(app.url)).apply {
                setTitle(app.name)
                setDescription("Downloading...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
            }
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = dm.enqueue(request)
            txtStatus.text = "Downloading... Check notification"
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            txtStatus.text = "Download failed: ${e.message}"
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                progressBar.visibility = View.GONE
                txtStatus.text = "Download complete! Installing..."
                val fileName = getDownloadedFileName()
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                if (file.exists()) installApk(file)
            }
        }
    }

    private fun getDownloadedFileName(): String {
        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val c = dm.query(query)
        if (c.moveToFirst()) {
            val name = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)) + ".apk"
            c.close()
            return name.replace(" ", "_")
        }
        c.close()
        return "app.apk"
    }

    private fun installApk(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
            val install = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(install)
            txtStatus.text = "Click INSTALL on popup!"
        } catch (e: Exception) {
            val uri = Uri.fromFile(file)
            val install = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(install)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
    }
}
