package com.y4n9b0.exoplayer

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.audio.AudioCapabilities
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.AudioSink
import com.google.android.exoplayer2.audio.DefaultAudioSink
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecAdapter
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.util.MediaClock
import com.google.android.exoplayer2.util.MediaClockExt

class MultiTrackAudioRenderer(
    context: Context,
    mediaCodecSelector: MediaCodecSelector,
    codecAdapterFactory: MediaCodecAdapter.Factory = MediaCodecAdapter.Factory.DEFAULT,
    enableDecoderFallback: Boolean = false,
    eventHandler: Handler? = null,
    eventListener: AudioRendererEventListener? = null,
    audioSink: AudioSink = DefaultAudioSink.Builder()
        .setAudioCapabilities(AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES)
        .build()
) : MediaClockExt, MediaCodecAudioRenderer(
    context,
    codecAdapterFactory,
    mediaCodecSelector,
    enableDecoderFallback,
    eventHandler,
    eventListener,
    audioSink
) {
    // 由于支持多条音频轨道使用多个音频 Renderer 同时输出，需要确定哪个音频 Renderer 提供 MediaClock
    private var provideMediaClock = false

    override fun getMediaClock(): MediaClock? {
        return if (provideMediaClock) this else null
    }

    override fun setProvideMediaClock(provideMediaClock: Boolean) {
        this.provideMediaClock = provideMediaClock
    }
}