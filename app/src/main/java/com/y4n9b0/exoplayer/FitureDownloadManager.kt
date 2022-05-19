package com.y4n9b0.exoplayer

import android.content.Context
import android.os.Environment
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor

class FitureDownloadManager private constructor(context: Context) {

    private val externalFilesDir: File = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir

    // Note: This should be a singleton in your app.
    val databaseProvider: DatabaseProvider by lazy {
        StandaloneDatabaseProvider(context)
    }

    val downloadCache: Cache by lazy {
        // A download cache should not evict media, so should use a NoopCacheEvictor.
        SimpleCache(
            externalFilesDir,
            NoOpCacheEvictor(),
            databaseProvider
        )
    }

    val httpDataSourceFactory: HttpDataSource.Factory by lazy {
        val userAgent = Util.getUserAgent(context, "FitureDownloader")
        DefaultHttpDataSource.Factory().apply { setUserAgent(userAgent) }
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
        ).apply { maxParallelDownloads = 3 }
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
            return instance ?: FitureDownloadManager(context).also { instance = it }
        }
    }
}