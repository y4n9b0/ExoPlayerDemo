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
package com.google.android.exoplayer2.ui;

import android.content.Context;
import android.graphics.Canvas;

import com.google.android.exoplayer2.text.Cue;

public interface SubtitlePainter {

  /** A factory for {@link SubtitlePainter} instances. */
  interface Factory {
    /** Default factory used in most cases. */
    Factory DEFAULT = DefaultSubtitlePainter::new;

    /** Creates a {@link SubtitlePainter} instance. */
    SubtitlePainter createSubtitlePainter(Context context);
  }

  /**
   * Draws the provided {@link Cue} into a canvas with the specified styling.
   *
   * <p>A call to this method is able to use cached results of calculations made during the previous
   * call, and so an instance of this class is able to optimize repeated calls to this method in
   * which the same parameters are passed.
   *
   * @param cue The cue to draw. sizes embedded within the cue should be applied. Otherwise, it is
   *     ignored.
   * @param style The style to use when drawing the cue text.
   * @param defaultTextSizePx The default text size to use when drawing the text, in pixels.
   * @param cueTextSizePx The embedded text size of this cue, in pixels.
   * @param bottomPaddingFraction The bottom padding fraction to apply when {@link Cue#line} is
   *     {@link Cue#DIMEN_UNSET}, as a fraction of the viewport height
   * @param canvas The canvas into which to draw.
   * @param cueBoxLeft The left position of the enclosing cue box.
   * @param cueBoxTop The top position of the enclosing cue box.
   * @param cueBoxRight The right position of the enclosing cue box.
   * @param cueBoxBottom The bottom position of the enclosing cue box.
   */
  void draw(
      Cue cue,
      CaptionStyleCompat style,
      float defaultTextSizePx,
      float cueTextSizePx,
      float bottomPaddingFraction,
      Canvas canvas,
      int cueBoxLeft,
      int cueBoxTop,
      int cueBoxRight,
      int cueBoxBottom);
}