package com.y4n9b0.exoplayer

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSource
import com.google.android.exoplayer2.extractor.flv.FlvExtractor
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSink
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.CacheKeyFactory
import com.google.android.exoplayer2.util.Util
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val TAG = "Bob"
    val ACTION_VIEW = "com.y4n9b0.exo.demo.action.VIEW"

    var url = "https://c4.biketo.com/article_video/20190917/4jsDf5pEGC.mp4"
    // var url = "https://dev-vod.fiture.com/64d4b7cfe64e44dab4c38babef3e3de6/3e84798be03a417c9bd6e70c246c4e68-d7a69d5f4eedaecc8f5a41310f7dca7e-ld.mp4"
    // led
    // var url = "http://cp-vod.qa.fiture.com/callback/play/add2e56c1e7b7df307a6cb826bdaee3b5a5496c7/video.m3u8"
    // led && vtt
    // var url = "http://cp-vod.qa.fiture.com/callback/play/0787a9e7a5d8cdd59123039d653123c8baf10c01/video.m3u8"
    // var url = "https://dev-vod.fiture.com/1834f6f999bc429580b553dd03bede20/997bf6a033d34ca5b8b4f4a7ee2a3a73-1e1a8c3c237291470a802e7df21b9ca9-od-S00000001-100000.m3u8"
    // var url = "https://dev-vod.fiture.com/7ec630630702455cbaed60cd1b1ceac4/269d6c9c439f4bd09d41c31cca3ef792-a709aafdbc5d9b5f987407d4a35671fc-od-S00000001-100000.m3u8"

    private val listener = object : Player.Listener {
        override fun onLedCues(cues: MutableList<Cue>) {
            super.onLedCues(cues)
            if (cues.isEmpty()) {
                Log.w(TAG, "Listener onLedCues empty")
            } else {
                Log.w(TAG, "Listener onLedCues ${cues[0]}")
            }
        }

        override fun onCues(cues: MutableList<Cue>) {
            super.onCues(cues)
            if (cues.isEmpty()) {
                Log.w(TAG, "Listener onCues empty")
            } else {
                Log.w(TAG, "Listener onCues ${cues[0].text}")
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            Log.e(TAG, buildErrorMessage(error))
        }

        private fun buildErrorMessage(error: PlaybackException): String {
            val newline = "\r\n"
            val indent = "  "
            val sb = StringBuilder()
                .append("onPlayerError, ExoPlaybackException: ${error.message}")
                .append(newline)
                .append(indent)
            var cause: Throwable? = error.cause
            var level = 1
            while (cause != null) {
                sb.append(newline)
                repeat(level) {
                    sb.append(indent)
                }
                sb.append("Cause $level: ${cause.message}")
                cause = cause.cause
                level++
            }
            return sb.toString()
        }
    }

    private val analyticsListener = object : AnalyticsListener {
        override fun onLedCues(eventTime: AnalyticsListener.EventTime, cues: MutableList<Cue>) {
            super.onLedCues(eventTime, cues)
            if (cues.isEmpty()) {
                Log.w(TAG, "AnalyticsListener onLedCues empty")
            } else {
                Log.w(TAG, "AnalyticsListener onLedCues ${cues[0]}")
            }
        }

        override fun onCues(eventTime: AnalyticsListener.EventTime, cues: MutableList<Cue>) {
            super.onCues(eventTime, cues)
            if (cues.isEmpty()) {
                Log.w(TAG, "AnalyticsListener onCues empty")
            } else {
                Log.w(TAG, "AnalyticsListener onCues ${cues[0].text}")
            }
        }

        override fun onAudioCodecError(
            eventTime: AnalyticsListener.EventTime,
            audioCodecError: Exception
        ) {
            super.onAudioCodecError(eventTime, audioCodecError)
            Log.e(TAG, "onAudioCodecError ${audioCodecError.message}")
        }

        override fun onAudioSinkError(
            eventTime: AnalyticsListener.EventTime,
            audioSinkError: Exception
        ) {
            super.onAudioSinkError(eventTime, audioSinkError)
            Log.e(TAG, "onAudioSinkError ${audioSinkError.message}")
        }

        override fun onLoadError(
            eventTime: AnalyticsListener.EventTime,
            loadEventInfo: LoadEventInfo,
            mediaLoadData: MediaLoadData,
            error: IOException,
            wasCanceled: Boolean
        ) {
            super.onLoadError(eventTime, loadEventInfo, mediaLoadData, error, wasCanceled)
            Log.e(TAG, "onLoadError ${error.message}")
        }

        override fun onPlayerError(
            eventTime: AnalyticsListener.EventTime,
            error: PlaybackException
        ) {
            super.onPlayerError(eventTime, error)
            Log.e(TAG, "AnalyticsListener onPlayerError ${error.message}")
        }

        override fun onVideoCodecError(
            eventTime: AnalyticsListener.EventTime,
            videoCodecError: Exception
        ) {
            super.onVideoCodecError(eventTime, videoCodecError)
            Log.e(TAG, "onPlayerError ${videoCodecError.message}")
        }

        override fun onMetadata(eventTime: AnalyticsListener.EventTime, metadata: Metadata) {
            val length = metadata.length()
            for (i in 0 until length) {
                val seiUserDataUnregistered = metadata[i] as? SeiUserDataUnregistered ?: continue
                seiUserDataUnregistered.payload?.apply {
                    Log.d(TAG, "onMetadata sei:$this")
                }
            }
        }
    }

    private val exoPlayer: ExoPlayer by lazy {
        val trackSelector = DefaultTrackSelector(this)
        trackSelector.parameters =
            DefaultTrackSelector.ParametersBuilder(this).setLedEnabled(true).setSrtEnabled(true)
                .build()
        val renderFactory = MultiTrackRenderFactory(this, 3)
        ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .setRenderersFactory(renderFactory)
            .build().apply {
                addListener(listener)
                addAnalyticsListener(analyticsListener)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bypassUrlByIntent()
        setContentView(R.layout.activity_main)

        val playerView = findViewById<PlayerView>(R.id.player_view)
        playerView.player = exoPlayer
        playerView.setSubtitlePainterFactory { FitureSubtitlePainter(it) }
        // exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.setMediaSource(createMediaSource(url))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    private fun createMediaSource(url: String, enableCaching: Boolean = true): MediaSource {
        return createMediaSource(Uri.parse(url), enableCaching)
    }

    private fun createMediaSource(uri: Uri, enableCaching: Boolean = true): MediaSource {
        val fdm = FitureDownloadManager.newInstance(this)
        val download: Download? = fdm.getDownloads()[uri.toString()]
        if (download != null) {
            Log.d("Bob", "createMediaSource download.state=${download.state}")
            // 手动下载完成的视频
            val dataSourceFactory = buildDataSourceFactory(enableCaching, true)
            return DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(download.request.toMediaItem())
        }

        val loadErrorHandlingPolicy: LoadErrorHandlingPolicy =
            object : DefaultLoadErrorHandlingPolicy() {
                override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                    return if (!NetworkUtil.isNetworkAvailable(this@MainActivity)) {
                        C.TIME_UNSET
                    } else super.getRetryDelayMsFor(loadErrorInfo)
                }
            }
        return if (uri.toString().startsWith("rtmp:")) {
            ProgressiveMediaSource.Factory(RtmpDataSource.Factory(), FlvExtractor.FACTORY)
                .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                .createMediaSource(MediaItem.fromUri(Uri.parse("$uri live=1")))
        } else {
            val mediaItem = MediaItem.fromUri(uri)
            val dataSourceFactory = buildDataSourceFactory(enableCaching)
            @C.ContentType val type = Util.inferContentType(uri, null)
            when (type) {
                C.TYPE_DASH -> DashMediaSource.Factory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                    .createMediaSource(mediaItem)
                C.TYPE_SS -> SsMediaSource.Factory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                    .createMediaSource(mediaItem)
                C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                    .createMediaSource(mediaItem)
                C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                    .createMediaSource(mediaItem)
                else -> DefaultMediaSourceFactory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                    .createMediaSource(mediaItem)
            }
        }
    }

    /**
     * 构建 DataSource 工厂
     * @param enableCaching 收否启用缓存
     * @param readOnly 缓存是否仅读（该值只有在 enableCaching=true 时有效），
     *        对于通过 [addDownload] 方式手动下载的视频，再次播放不需要缓存可写，设置 readOnly=true
     *        对于边播边缓存的视频，ExoPlayer 自动处理了再次播放的缓存逻辑，不需要设置 readOnly=true
     */
    private fun buildDataSourceFactory(
        enableCaching: Boolean,
        readOnly: Boolean = false
    ): DataSource.Factory {
        val fdm = FitureDownloadManager.newInstance(this)
        val upstreamFactory = DefaultDataSource.Factory(this, fdm.httpDataSourceFactory)
        return if (enableCaching) {
            buildCacheDataSourceFactory(upstreamFactory, fdm.downloadCache, readOnly)
        } else {
            upstreamFactory
        }
    }

    private fun buildCacheDataSourceFactory(
        upstreamFactory: DataSource.Factory,
        cache: Cache,
        readOnly: Boolean = false
    ): DataSource.Factory {
        // setCacheWriteDataSinkFactory(null) to disable writing if read only or device storage low.
        val cacheWriteDataSinkFactory: DataSink.Factory? =
            if (readOnly || isDeviceStorageLow(this)) null
            else CacheDataSink.Factory().setCache(cache).setFragmentSize(Long.MAX_VALUE)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheReadDataSourceFactory(FileDataSource.Factory())
            .setCacheWriteDataSinkFactory(cacheWriteDataSinkFactory)
            .setCacheKeyFactory(CacheKeyFactory.DEFAULT)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private fun isDeviceStorageLow(context: Context): Boolean {
        // Check for low storage.  If there is low storage, the media will not be cached.
        val lowstorageFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
        val hasLowStorage = context.registerReceiver(null, lowstorageFilter) != null
        return hasLowStorage
    }

    // https://exoplayer.dev/downloading-media.html#adding-a-download
    private fun addDownload(url: String) {
        val downloadRequest = DownloadRequest.Builder(url, Uri.parse(url)).build()
        DownloadService.sendAddDownload(
            this@MainActivity,
            FitureDownloadService::class.java,
            downloadRequest,
            /* foreground= */ false
        )
    }

    // https://exoplayer.dev/downloading-media.html#downloading-and-playing-adaptive-streams
    // private fun addDownload(url: String) {
    //     val downloadHelper = DownloadHelper.forMediaItem(this, MediaItem.fromUri(Uri.parse(url)))
    //     downloadHelper.prepare(object : DownloadHelper.Callback {
    //         override fun onPrepared(helper: DownloadHelper) {
    //             // downloadHelper.replaceTrackSelections()
    //             val downloadRequest = downloadHelper.getDownloadRequest(null)
    //             DownloadService.sendAddDownload(
    //                 this@MainActivity,
    //                 FitureDownloadService::class.java,
    //                 downloadRequest,
    //                 /* foreground= */ false
    //             )
    //             downloadHelper.release()
    //         }
    //
    //         override fun onPrepareError(helper: DownloadHelper, e: IOException) {
    //             // ignore
    //         }
    //     })
    // }

    private fun removeDownload(url: String) {
        DownloadService.sendRemoveDownload(
            this,
            FitureDownloadService::class.java,
            url,
            /* foreground= */ false
        )
    }

    /**
     * 使用方式 adb shell am start [-a <ACTION>] [-d <DATA_URI>]
     *
     * adb shell am start -a com.y4n9b0.exo.demo.action.VIEW -d https://y4n9b0.github.io/styles/images/hengxiaomei/hengxiaomei_720p.mp4
     */
    private fun bypassUrlByIntent() {
        if (intent != null && intent.action == ACTION_VIEW) {
            intent.dataString?.apply {
                url = this
            }
        }
    }
}