/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.extractor.flv;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.NalUnitUtil;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.video.AvcConfig;

/** Parses video tags from an FLV stream and extracts H.264 nal units. */
/* package */ final class VideoTagPayloadReader extends TagPayloadReader {

  private static final String TAG = "VideoTagPayloadReader";

  // Video codec.
  private static final int VIDEO_CODEC_AVC = 7;

  // Frame types.
  private static final int VIDEO_FRAME_KEYFRAME = 1;
  private static final int VIDEO_FRAME_VIDEO_INFO = 5;

  // Packet types.
  private static final int AVC_PACKET_TYPE_SEQUENCE_HEADER = 0;
  private static final int AVC_PACKET_TYPE_AVC_NALU = 1;

  // SEI payload types
  private static final int SEI_PAYLOAD_TYPE_USER_DATA_UNREGISTERED = 5;

  // Temporary arrays.
  private final ParsableByteArray nalStartCode;
  private final ParsableByteArray nalLength;
  private final ParsableByteArray seiBuffer;
  private int nalUnitLengthFieldLength;

  // State variables.
  private boolean hasOutputFormat;
  private boolean hasOutputKeyframe;
  private int frameType;

  private final TrackOutput seiUserDataUnregisteredOutput;

  /**
   *
   * @param output A {@link TrackOutput} to which samples should be written.
   * @param seiUserDataUnregisteredOutput A {@link TrackOutput} to which sei user data unregistered samples should be written.
   */
  public VideoTagPayloadReader(TrackOutput output, TrackOutput seiUserDataUnregisteredOutput) {
    super(output);
    this.seiUserDataUnregisteredOutput = seiUserDataUnregisteredOutput;
    nalStartCode = new ParsableByteArray(NalUnitUtil.NAL_START_CODE);
    nalLength = new ParsableByteArray(4);
    seiBuffer = new ParsableByteArray();
  }

  @Override
  public void seek() {
    hasOutputKeyframe = false;
  }

  @Override
  protected boolean parseHeader(ParsableByteArray data) throws UnsupportedFormatException {
    int header = data.readUnsignedByte();
    int frameType = (header >> 4) & 0x0F;
    int videoCodec = (header & 0x0F);
    // Support just H.264 encoded content.
    if (videoCodec != VIDEO_CODEC_AVC) {
      throw new UnsupportedFormatException("Video format not supported: " + videoCodec);
    }
    this.frameType = frameType;
    return (frameType != VIDEO_FRAME_VIDEO_INFO);
  }

  @Override
  protected boolean parsePayload(ParsableByteArray data, long timeUs) throws ParserException {
    int packetType = data.readUnsignedByte();
    int compositionTimeMs = data.readInt24();

    timeUs += compositionTimeMs * 1000L;
    // Parse avc sequence header in case this was not done before.
    if (packetType == AVC_PACKET_TYPE_SEQUENCE_HEADER && !hasOutputFormat) {
      ParsableByteArray videoSequence = new ParsableByteArray(new byte[data.bytesLeft()]);
      data.readBytes(videoSequence.getData(), 0, data.bytesLeft());
      AvcConfig avcConfig = AvcConfig.parse(videoSequence);
      nalUnitLengthFieldLength = avcConfig.nalUnitLengthFieldLength;
      // Construct and output the format.
      Format format =
          new Format.Builder()
              .setSampleMimeType(MimeTypes.VIDEO_H264)
              .setCodecs(avcConfig.codecs)
              .setWidth(avcConfig.width)
              .setHeight(avcConfig.height)
              .setPixelWidthHeightRatio(avcConfig.pixelWidthHeightRatio)
              .setInitializationData(avcConfig.initializationData)
              .build();
      output.format(format);
      Format seiFormat = new Format.Builder()
          .setSampleMimeType(MimeTypes.APPLICATION_SEI_USER_DATA_UNREGISTERED)
          .build();
      seiUserDataUnregisteredOutput.format(seiFormat);
      hasOutputFormat = true;
      return false;
    } else if (packetType == AVC_PACKET_TYPE_AVC_NALU && hasOutputFormat) {
      boolean isKeyframe = frameType == VIDEO_FRAME_KEYFRAME;
      if (!hasOutputKeyframe && !isKeyframe) {
        return false;
      }
      // TODO: Deduplicate with Mp4Extractor.
      // Zero the top three bytes of the array that we'll use to decode nal unit lengths, in case
      // they're only 1 or 2 bytes long.
      byte[] nalLengthData = nalLength.getData();
      nalLengthData[0] = 0;
      nalLengthData[1] = 0;
      nalLengthData[2] = 0;
      int nalUnitLengthFieldLengthDiff = 4 - nalUnitLengthFieldLength;
      // NAL units are length delimited, but the decoder requires start code delimited units.
      // Loop until we've written the sample to the track output, replacing length delimiters with
      // start codes as we encounter them.
      int bytesWritten = 0;
      int bytesToWrite;
      while (data.bytesLeft() > 0) {
        // Read the NAL length so that we know where we find the next one.
        data.readBytes(nalLength.getData(), nalUnitLengthFieldLengthDiff, nalUnitLengthFieldLength);
        nalLength.setPosition(0);
        bytesToWrite = nalLength.readUnsignedIntToInt();

        // Write a start code for the current NAL unit.
        nalStartCode.setPosition(0);
        output.sampleData(nalStartCode, 4);
        bytesWritten += 4;

        // Write the payload of the NAL unit.
        output.sampleData(data, bytesToWrite);
        bytesWritten += bytesToWrite;

        final byte[] nalUnitData = new byte[bytesToWrite];
        System.arraycopy(data.getData(), data.getPosition() - bytesToWrite, nalUnitData, 0, bytesToWrite);
        if (NalUnitUtil.isNalUnitSei(MimeTypes.VIDEO_H264, nalUnitData[0])) {
          seiBuffer.reset(nalUnitData);
          // Unescape and process the SEI NAL unit.
          int unescapedLength = NalUnitUtil.unescapeStream(seiBuffer.getData(), seiBuffer.limit());
          seiBuffer.setLimit(unescapedLength);
          // Skip NAL unit type byte
          seiBuffer.skipBytes(1);
          while (seiBuffer.bytesLeft() > 1 /* last byte will be rbsp_trailing_bits */) {
            int payloadType = readNon255TerminatedValue(seiBuffer);
            int payloadSize = readNon255TerminatedValue(seiBuffer);
            int nextPayloadPosition = seiBuffer.getPosition() + payloadSize;
            // Process the payload.
            if (payloadSize == -1 || payloadSize > seiBuffer.bytesLeft()) {
              // This might occur if we're trying to read an encrypted SEI NAL unit.
              Log.w(TAG, "parsePayload - Skipping remainder of malformed SEI NAL unit.");
              nextPayloadPosition = seiBuffer.limit();
            } else if (payloadType == SEI_PAYLOAD_TYPE_USER_DATA_UNREGISTERED && payloadSize > 16) {
              // Skip uuid(16)
              seiBuffer.skipBytes(16);
              seiUserDataUnregisteredOutput.sampleData(seiBuffer, payloadSize - 16);
              seiUserDataUnregisteredOutput.sampleMetadata(
                  timeUs, isKeyframe ? C.BUFFER_FLAG_KEY_FRAME : 0, payloadSize - 16, 0, null);
            }
            seiBuffer.setPosition(nextPayloadPosition);
          }
        }
      }
      output.sampleMetadata(
          timeUs, isKeyframe ? C.BUFFER_FLAG_KEY_FRAME : 0, bytesWritten, 0, null);
      hasOutputKeyframe = true;
      return true;
    } else {
      return false;
    }
  }

  /**
   * Reads a value from the provided buffer consisting of zero or more 0xFF bytes followed by a
   * terminating byte not equal to 0xFF. The returned value is ((0xFF * N) + T), where N is the
   * number of 0xFF bytes and T is the value of the terminating byte.
   *
   * @param buffer The buffer from which to read the value.
   * @return The read value, or -1 if the end of the buffer is reached before a value is read.
   */
  private static int readNon255TerminatedValue(ParsableByteArray buffer) {
    int b;
    int value = 0;
    do {
      if (buffer.bytesLeft() == 0) {
        return -1;
      }
      b = buffer.readUnsignedByte();
      value += b;
      } while (b == 0xFF);
    return value;
  }
}
