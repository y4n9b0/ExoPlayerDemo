package com.y4n9b0.exoplayer

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
import com.google.android.exoplayer2.offline.Download
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
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.CacheKeyFactory
import com.google.android.exoplayer2.util.Util
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val TAG = "Bob"
    val ACTION_VIEW = "com.y4n9b0.exo.demo.action.VIEW"
    var url = "http://cp-vod.qa.fiture.com/callback/play/add2e56c1e7b7df307a6cb826bdaee3b5a5496c7/video.m3u8"
    // var url = "https://dev-vod.fiture.com/1834f6f999bc429580b553dd03bede20/997bf6a033d34ca5b8b4f4a7ee2a3a73-1e1a8c3c237291470a802e7df21b9ca9-od-S00000001-100000.m3u8"
    // var url = "https://dev-vod.fiture.com/7ec630630702455cbaed60cd1b1ceac4/269d6c9c439f4bd09d41c31cca3ef792-a709aafdbc5d9b5f987407d4a35671fc-od-S00000001-100000.m3u8"

    private val listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            Log.e(TAG, "Listener onPlayerError ${error.message}")
        }
    }

    private val analyticsListener = object : AnalyticsListener {
        override fun onAudioCodecError(eventTime: AnalyticsListener.EventTime, audioCodecError: Exception) {
            super.onAudioCodecError(eventTime, audioCodecError)
            Log.e(TAG, "onAudioCodecError ${audioCodecError.message}")
        }

        override fun onAudioSinkError(eventTime: AnalyticsListener.EventTime, audioSinkError: Exception) {
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

        override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
            super.onPlayerError(eventTime, error)
            Log.e(TAG, "AnalyticsListener onPlayerError ${error.message}")
        }

        override fun onVideoCodecError(eventTime: AnalyticsListener.EventTime, videoCodecError: Exception) {
            super.onVideoCodecError(eventTime, videoCodecError)
            Log.e(TAG, "onPlayerError ${videoCodecError.message}")
        }
    }

    private val exoPlayer: ExoPlayer by lazy {
        val trackSelector = MultiTrackSelector(this)
        val renderFactory = MultiTrackRenderFactory(this, 3)
        ExoPlayer.Builder(this)
            // .setTrackSelector(trackSelector)
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
        if (download != null && download.state == Download.STATE_COMPLETED) {
            // 手动下载完成的视频
            val dataSourceFactory = buildDataSourceFactory(enableCaching, true)
            return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
        }

        val loadErrorHandlingPolicy: LoadErrorHandlingPolicy = object : DefaultLoadErrorHandlingPolicy() {
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
                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy).createMediaSource(mediaItem)
                C.TYPE_SS -> SsMediaSource.Factory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy).createMediaSource(mediaItem)
                C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy).createMediaSource(mediaItem)
                C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy).createMediaSource(mediaItem)
                else -> DefaultMediaSourceFactory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy).createMediaSource(mediaItem)
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
    private fun buildDataSourceFactory(enableCaching: Boolean, readOnly: Boolean = false): DataSource.Factory {
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
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheReadDataSourceFactory(FileDataSource.Factory())
            .setCacheKeyFactory(CacheKeyFactory.DEFAULT)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            .apply {
                if (readOnly) setCacheWriteDataSinkFactory(null) // Disable writing.
            }
    }

    private fun addDownload(url: String) {
        val downloadRequest = DownloadRequest.Builder(url, Uri.parse(url)).build()
        DownloadService.sendAddDownload(
            this,
            FitureDownloadService::class.java,
            downloadRequest,
            /* foreground= */ false
        )
    }

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