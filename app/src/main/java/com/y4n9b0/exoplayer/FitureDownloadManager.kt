package com.y4n9b0.exoplayer

import android.content.Context
import android.os.Environment
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor

class FitureDownloadManager private constructor(context: Context) {

    private val externalFilesDir: File = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        ?: File(context.filesDir, Environment.DIRECTORY_MOVIES)

    // Note: This should be a singleton in your app.
    val databaseProvider: DatabaseProvider by lazy {
        StandaloneDatabaseProvider(context)
    }

    val downloadCache: Cache by lazy {
        // A download cache keep only one media file at most.
        SimpleCache(
            externalFilesDir,
            LruCacheEvictor(1 shl 30, 1), // 缓存缓存最大空间 1G 且最多 1 个视频
            databaseProvider
        )
    }

    val httpDataSourceFactory: HttpDataSource.Factory by lazy {
        val userAgent = Util.getUserAgent(context, "FitureDownloader")
        DefaultHttpDataSource.Factory().setUserAgent(userAgent)
    }

    val okHttpDataSourceFactory: HttpDataSource.Factory by lazy {
        val userAgent = Util.getUserAgent(context, "FitureDownloader")
        val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()
        OkHttpDataSource.Factory(okHttpClient).setUserAgent(userAgent)
    }

    val downloadManager: DownloadManager by lazy {
        // Choose an executor for downloading data. Using Runnable::run will cause each download task to
        // download data on its own thread. Passing an executor that uses multiple threads will speed up
        // download tasks that can be split into smaller parts for parallel execution. Applications that
        // already have an executor for background downloads may wish to reuse their existing executor.
        val downloadExecutor = Executor { obj: Runnable -> obj.run() }

        // Create the download manager.
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            httpDataSourceFactory,
            downloadExecutor
        ).apply {
            maxParallelDownloads = 3
            // addListener(object : DownloadManager.Listener {})
        }
    }

    fun getDownloads(): HashMap<String, Download> {
        val downloads: HashMap<String, Download> = HashMap()
        try {
            downloadManager.downloadIndex.getDownloads().use { downloadCursor ->
                while (downloadCursor.moveToNext()) {
                    val download: Download = downloadCursor.download
                    downloads[download.request.id] = download
                }
            }
        } catch (ignore: IOException) {
        }
        return downloads
    }

    companion object {
        private var instance: FitureDownloadManager? = null

        @Synchronized
        fun newInstance(context: Context): FitureDownloadManager {
            return instance ?: FitureDownloadManager(context.applicationContext).also { instance = it }
        }
    }
}