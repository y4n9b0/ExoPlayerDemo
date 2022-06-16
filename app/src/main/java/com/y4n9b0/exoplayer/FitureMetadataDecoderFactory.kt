package com.y4n9b0.exoplayer

import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.metadata.MetadataDecoder
import com.google.android.exoplayer2.metadata.MetadataDecoderFactory
import com.google.android.exoplayer2.metadata.dvbsi.AppInfoTableDecoder
import com.google.android.exoplayer2.metadata.emsg.EventMessageDecoder
import com.google.android.exoplayer2.metadata.icy.IcyDecoder
import com.google.android.exoplayer2.metadata.id3.Id3Decoder
import com.google.android.exoplayer2.metadata.scte35.SpliceInfoDecoder
import com.google.android.exoplayer2.util.MimeTypes

/**
 * base on [MetadataDecoderFactory.DEFAULT]
 */
class FitureMetadataDecoderFactory : MetadataDecoderFactory {

    override fun supportsFormat(format: Format): Boolean {
        val mimeType = format.sampleMimeType
        return MimeTypes.APPLICATION_ID3 == mimeType
            || MimeTypes.APPLICATION_EMSG == mimeType
            || MimeTypes.APPLICATION_SCTE35 == mimeType
            || MimeTypes.APPLICATION_ICY == mimeType
            || MimeTypes.APPLICATION_AIT == mimeType
            || MimeTypes.APPLICATION_SEI_USER_DATA_UNREGISTERED == mimeType
    }

    override fun createDecoder(format: Format): MetadataDecoder {
        val mimeType = format.sampleMimeType
        if (mimeType != null) {
            when (mimeType) {
                MimeTypes.APPLICATION_ID3 -> return Id3Decoder()
                MimeTypes.APPLICATION_EMSG -> return EventMessageDecoder()
                MimeTypes.APPLICATION_SCTE35 -> return SpliceInfoDecoder()
                MimeTypes.APPLICATION_ICY -> return IcyDecoder()
                MimeTypes.APPLICATION_AIT -> return AppInfoTableDecoder()
                MimeTypes.APPLICATION_SEI_USER_DATA_UNREGISTERED -> return SeiUserDataUnregisteredDecoder()
                else -> {}
            }
        }
        throw IllegalArgumentException(
            "Attempted to create decoder for unsupported MIME type: $mimeType"
        )
    }
}