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

class MultiTrackAudioRender(
    private val trackIndex: Int,
    context: Context,
    mediaCodecSelector: MediaCodecSelector,
    codecAdapterFactory: MediaCodecAdapter.Factory = MediaCodecAdapter.Factory.DEFAULT,
    enableDecoderFallback: Boolean = false,
    eventHandler: Handler? = null,
    eventListener: AudioRendererEventListener? = null,
    audioSink: AudioSink = DefaultAudioSink.Builder()
        .setAudioCapabilities(AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES)
        .build()
) : MediaCodecAudioRenderer(
    context,
    codecAdapterFactory,
    mediaCodecSelector,
    enableDecoderFallback,
    eventHandler,
    eventListener,
    audioSink
) {
    override fun getMediaClock(): MediaClock? = if (trackIndex == 0) super.getMediaClock() else null
}