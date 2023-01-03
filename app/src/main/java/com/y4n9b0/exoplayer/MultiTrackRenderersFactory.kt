package com.y4n9b0.exoplayer

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.audio.AudioCapabilities
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.AudioSink
import com.google.android.exoplayer2.audio.DefaultAudioSink
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.metadata.MetadataRenderer
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.util.Util
import kotlin.math.max

class MultiTrackRenderersFactory(
    context: Context,
    private val multiAudioTrackCount: Int
) : DefaultRenderersFactory(context) {

    override fun buildAudioRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink,
        eventHandler: Handler,
        eventListener: AudioRendererEventListener,
        out: ArrayList<Renderer>
    ) {
        repeat(max(1, multiAudioTrackCount)) {
            out.add(
                MultiTrackAudioRenderer(
                    context = context,
                    mediaCodecSelector = mediaCodecSelector,
                    codecAdapterFactory = codecAdapterFactory,
                    enableDecoderFallback = enableDecoderFallback,
                    eventHandler = eventHandler,
                    eventListener = eventListener,
                    audioSink = (
                        buildAudioSink()
                            ?: DefaultAudioSink.Builder()
                                .setAudioCapabilities(AudioCapabilities.getCapabilities(context))
                                .build()
                        ).apply { audioSessionId = Util.generateAudioSessionIdV21(context) }
                )
            )
        }

        if (extensionRendererMode == EXTENSION_RENDERER_MODE_OFF) {
            return
        }
        var extensionRendererIndex = out.size
        if (extensionRendererMode == EXTENSION_RENDERER_MODE_PREFER) {
            extensionRendererIndex--
        }

        try {
            // Full class names used for constructor args so the LINT rule triggers if any of them move.
            val clazz = Class.forName("com.google.android.exoplayer2.ext.opus.LibopusAudioRenderer")
            val constructor = clazz.getConstructor(
                Handler::class.java,
                AudioRendererEventListener::class.java,
                AudioSink::class.java
            )
            val renderer = constructor.newInstance(eventHandler, eventListener, audioSink) as Renderer
            out.add(extensionRendererIndex++, renderer)
        } catch (e: ClassNotFoundException) {
            // Expected if the app was built without the extension.
        } catch (e: Exception) {
            // The extension is present, but instantiation failed.
            throw RuntimeException("Error instantiating Opus extension", e)
        }

        try {
            // Full class names used for constructor args so the LINT rule triggers if any of them move.
            val clazz = Class.forName("com.google.android.exoplayer2.ext.flac.LibflacAudioRenderer")
            val constructor = clazz.getConstructor(
                Handler::class.java,
                AudioRendererEventListener::class.java,
                AudioSink::class.java
            )
            val renderer = constructor.newInstance(eventHandler, eventListener, audioSink) as Renderer
            out.add(extensionRendererIndex++, renderer)
        } catch (e: ClassNotFoundException) {
            // Expected if the app was built without the extension.
        } catch (e: Exception) {
            // The extension is present, but instantiation failed.
            throw RuntimeException("Error instantiating FLAC extension", e)
        }

        try {
            // Full class names used for constructor args so the LINT rule triggers if any of them move.
            val clazz = Class.forName("com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer")
            val constructor = clazz.getConstructor(
                Handler::class.java,
                AudioRendererEventListener::class.java,
                AudioSink::class.java
            )
            val renderer = constructor.newInstance(eventHandler, eventListener, audioSink) as Renderer
            out.add(extensionRendererIndex++, renderer)
        } catch (e: ClassNotFoundException) {
            // Expected if the app was built without the extension.
        } catch (e: Exception) {
            // The extension is present, but instantiation failed.
            throw RuntimeException("Error instantiating FFmpeg extension", e)
        }
    }

    override fun buildTextRenderers(
        context: Context,
        output: TextOutput,
        outputLooper: Looper,
        extensionRendererMode: Int,
        out: ArrayList<Renderer>
    ) {
        super.buildTextRenderers(context, output, outputLooper, extensionRendererMode, out)
        out.add(FitureLedRenderer(output, outputLooper))
    }

    override fun buildMetadataRenderers(
        context: Context,
        output: MetadataOutput,
        outputLooper: Looper,
        extensionRendererMode: Int,
        out: ArrayList<Renderer>
    ) {
        out.add(MetadataRenderer(output, outputLooper, FitureMetadataDecoderFactory()))
    }
}