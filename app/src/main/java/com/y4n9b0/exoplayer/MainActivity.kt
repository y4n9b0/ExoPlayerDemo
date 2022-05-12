package com.y4n9b0.exoplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView

class MainActivity : AppCompatActivity() {

    val ACTION_VIEW = "com.y4n9b0.exo.demo.action.VIEW"
    var url = "https://c4.biketo.com/article_video/20190917/4jsDf5pEGC.mp4"

    val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(this).build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bypassUrlByIntent()
        setContentView(R.layout.activity_main)

        val playerView = findViewById<PlayerView>(R.id.player_view)
        playerView.player = exoPlayer
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        exoPlayer.play()
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