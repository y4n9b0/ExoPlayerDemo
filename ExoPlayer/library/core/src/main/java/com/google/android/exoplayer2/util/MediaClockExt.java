package com.google.android.exoplayer2.util;
/**
 * 支持动态设置其是否提供同步时钟
 */
public interface MediaClockExt extends MediaClock {
    /**
     * 设置是否提供同步时钟
     *
     * @param provideMediaClock 是否提供同步时钟
     */
    void setProvideMediaClock(boolean provideMediaClock);
}
