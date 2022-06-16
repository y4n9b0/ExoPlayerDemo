package com.y4n9b0.exoplayer

import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.MetadataInputBuffer
import com.google.android.exoplayer2.metadata.SimpleMetadataDecoder
import com.google.android.exoplayer2.util.ParsableByteArray
import java.nio.ByteBuffer

class SeiUserDataUnregisteredDecoder : SimpleMetadataDecoder() {
    private var lastSei: String? = null

    override fun decode(inputBuffer: MetadataInputBuffer, buffer: ByteBuffer): Metadata? {
        val sei: String? = ParsableByteArray(buffer.array(), buffer.limit()).readNullTerminatedString()
        return if (sei != null && sei != lastSei) {
            lastSei = sei
            Metadata(SeiUserDataUnregistered(sei))
        } else {
            null
        }
    }
}