package com.y4n9b0.exoplayer

import android.os.Looper
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.RendererCapabilities
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.text.TextRenderer

class FitureLedRenderer(
    output: TextOutput,
    outputLooper: Looper?
) : TextRenderer(output, outputLooper) {

    override fun supportsFormat(format: Format): Int {
        return RendererCapabilities.create(if (format.isLed) C.FORMAT_HANDLED else C.FORMAT_UNSUPPORTED_TYPE)
    }

    override fun invokeUpdateOutputInternal(cues: MutableList<Cue>) {
        output.onLedCues(cues)
    }
}
