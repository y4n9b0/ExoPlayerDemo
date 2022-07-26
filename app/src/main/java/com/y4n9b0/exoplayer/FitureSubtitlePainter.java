package com.y4n9b0.exoplayer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Xfermode;
import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;

import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.ui.CaptionStyleCompat;
import com.google.android.exoplayer2.ui.SubtitlePainter;
import com.google.android.exoplayer2.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Paints subtitle {@link Cue}s custom by Fiture, just handle text within cue only,
 * ignore bitmap or embedded style/spans/alignment/fontSize/size/line/position/etc..
 *
 * <a href="https://www.w3.org/TR/webvtt1/#cues">Cue</a>.
 */
public final class FitureSubtitlePainter implements SubtitlePainter {

    private static final String TAG = "FitureSubtitlePainter";

    private final Context context;

    // Styled dimensions.
    private final float spacingMult;
    private final float spacingAdd;

    private final TextPaint textPaint;
    private final Paint paint;

    // Previous input variables.
    private CharSequence cueText;
    private final int foregroundColor = Color.WHITE;
    private final int backgroundColor = Color.TRANSPARENT;
    private final int windowColor = Color.argb(128, 0, 0, 0); // 半透明黑色
    private float defaultTextSizePx;
    private float bottomPaddingFraction;
    private int parentLeft;
    private int parentTop;
    private int parentRight;
    private int parentBottom;

    // Derived drawing variables.
    private StaticLayout textLayout;
    private int textLeft;
    private int textTop;
    private final int textPaddingX;
    private final int textPaddingY;
    private final int textAvailableWidth;

    private final Rect window = new Rect();
    private final List<Integer> lineWidths = new ArrayList<>();
    private final List<Rect> reusableExcludeRects = Arrays.asList(new Rect(), new Rect()); // 当前绝大多数字幕只有两行，重用两个实例足矣
    private final List<Rect> excludeRects = new ArrayList<>();

    @SuppressWarnings("ResourceType")
    public FitureSubtitlePainter(Context context) {
        this.context = context;
        textPaddingX = dp2px(16);           // 字幕水平间距 16dp
        textPaddingY = dp2px(6);            // 字幕垂直间距 6dp
        textAvailableWidth = dp2px(256);    // 字幕宽度(含间距) 256dp

        int[] viewAttr = {android.R.attr.lineSpacingExtra, android.R.attr.lineSpacingMultiplier};
        TypedArray styledAttributes = context.obtainStyledAttributes(null, viewAttr, 0, 0);
        spacingAdd = styledAttributes.getDimensionPixelSize(0, textPaddingY << 1);
        spacingMult = styledAttributes.getFloat(1, 1);
        styledAttributes.recycle();

        textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Style.FILL);
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
    @Override
    public void draw(
            Cue cue,
            CaptionStyleCompat style,
            float defaultTextSizePx,
            float cueTextSizePx,
            float bottomPaddingFraction,
            Canvas canvas,
            int cueBoxLeft,
            int cueBoxTop,
            int cueBoxRight,
            int cueBoxBottom) {
        if (TextUtils.isEmpty(cue.text)) {
            // Nothing to draw.
            return;
        }
        if (areCharSequencesEqual(this.cueText, cue.text)
                && this.defaultTextSizePx == defaultTextSizePx
                && this.bottomPaddingFraction == bottomPaddingFraction
                && this.parentLeft == cueBoxLeft
                && this.parentTop == cueBoxTop
                && this.parentRight == cueBoxRight
                && this.parentBottom == cueBoxBottom) {
            // We can use the cached layout.
            drawTextLayout(canvas);
            return;
        }

        this.cueText = cue.text;
        this.defaultTextSizePx = defaultTextSizePx;
        this.bottomPaddingFraction = bottomPaddingFraction;
        this.parentLeft = cueBoxLeft;
        this.parentTop = cueBoxTop;
        this.parentRight = cueBoxRight;
        this.parentBottom = cueBoxBottom;

        setupTextLayout();
        drawTextLayout(canvas);
    }

    private void setupTextLayout() {
        int parentWidth = parentRight - parentLeft;
        int parentHeight = parentBottom - parentTop;

        textPaint.setTextSize(defaultTextSizePx);

        int availableWidth = Math.min(parentWidth, textAvailableWidth) - textPaddingX * 2;
        if (availableWidth <= 0) {
            Log.w(TAG, "Skipped drawing subtitle cue (insufficient space)");
            return;
        }

        CharSequence cueText = this.cueText;
        // Remove embedded styling or font size.
        cueText = cueText.toString();

        if (Color.alpha(backgroundColor) > 0) {
            SpannableStringBuilder newCueText = new SpannableStringBuilder(cueText);
            newCueText.setSpan(new BackgroundColorSpan(backgroundColor), 0, newCueText.length(), Spanned.SPAN_PRIORITY);
            cueText = newCueText;
        }

        Alignment textAlignment = Alignment.ALIGN_NORMAL;
        textLayout = new StaticLayout(cueText, textPaint, availableWidth, textAlignment, spacingMult, spacingAdd, true);
        int textHeight = textLayout.getHeight();
        int textWidth = 0;
        int lineCount = textLayout.getLineCount();
        lineWidths.clear();
        for (int i = 0; i < lineCount; i++) {
            int currentLineWidth = (int) Math.ceil(textLayout.getLineWidth(i));
            textWidth = Math.max(currentLineWidth, textWidth);
            lineWidths.add(currentLineWidth);
        }
        if (textWidth < availableWidth) {
            textWidth = availableWidth;
        }

        int textLeft = parentLeft + textPaddingX;
        int textTop = parentBottom - textPaddingY - textHeight - (int) (parentHeight * bottomPaddingFraction);

        // Update the derived drawing variables.
        this.textLayout = new StaticLayout(cueText, textPaint, textWidth, textAlignment, spacingMult, spacingAdd, true);
        this.textLeft = textLeft;
        this.textTop = textTop;
    }

    private void drawTextLayout(Canvas canvas) {
        StaticLayout layout = textLayout;
        if (layout == null) {
            // Nothing to draw.
            return;
        }

        int saveCount = canvas.save();
        canvas.translate(textLeft, textTop);

        if (Color.alpha(windowColor) > 0) {
            paint.setColor(windowColor);
            window.set(-textPaddingX, -textPaddingY, layout.getWidth() + textPaddingX,
                    layout.getHeight() + textPaddingY);
            int lineCount = lineWidths.size();
            int size = excludeRects.size();
            if (size < lineCount) {
                for (int i = size; i < lineCount; i++) {
                    excludeRects.add(i < reusableExcludeRects.size() ? reusableExcludeRects.get(i) : new Rect());
                }
            } else if (size > lineCount) {
                for (int i = size; i > lineCount; i--) {
                    excludeRects.remove(i - 1);
                }
            }
            float lineHeight = (float) layout.getHeight() / lineCount;
            int right = layout.getWidth() + textPaddingX;
            for (int i = 0; i < lineCount; i++) {
                int left = lineWidths.get(i) + textPaddingX;
                int top = (int) Math.ceil(lineHeight * i);
                int bottom = (int) Math.ceil(lineHeight * (i + 1));
                if (i == 0) top -= textPaddingY;
                if (i == lineCount - 1) bottom += textPaddingY;
                excludeRects.get(i).set(left, top, right, bottom);
            }
            drawWindowRect(canvas, paint, window, excludeRects);
        }

        textPaint.setColor(foregroundColor);
        textPaint.setStyle(Style.FILL);
        layout.draw(canvas);
        textPaint.setShadowLayer(0, 0, 0, 0);

        canvas.restoreToCount(saveCount);
    }

    private int dp2px(float dp) {
        return (int) (context.getResources().getDisplayMetrics().density * dp + 0.5f);
    }

    /**
     * This method is used instead of {@link TextUtils#equals(CharSequence, CharSequence)} because the
     * latter only checks the text of each sequence, and does not check for equality of styling that
     * may be embedded within the {@link CharSequence}s.
     */
    @SuppressWarnings("UndefinedEquals")
    private static boolean areCharSequencesEqual(CharSequence first, CharSequence second) {
        // Some CharSequence implementations don't perform a cheap referential equality check in their
        // equals methods, so we perform one explicitly here.
        return Objects.equals(first, second);
    }

    private static void drawWindowRect(Canvas canvas, Paint paint, Rect window, List<Rect> excludeRects) {
        canvas.drawRect(window, paint);
        if (excludeRects.size() == 0) return;
        Xfermode defaultMode = paint.getXfermode();
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        paint.setColor(Color.argb(255, 0, 0, 0));
        for (Rect rect : excludeRects) {
            canvas.drawRect(rect, paint);
        }
        paint.setXfermode(defaultMode);
    }
}
