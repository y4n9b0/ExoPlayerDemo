package com.y4n9b0.exoplayer

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheEvictor
import com.google.android.exoplayer2.upstream.cache.CacheSpan
import java.util.*

class LruCacheEvictor(val maxCount: Int) : CacheEvictor {

    var currentCount = 0
    val leastRecentlyUsed: TreeSet<CacheSpan> = TreeSet { lhs, rhs ->
        val lastTouchTimestampDelta: Long = lhs.lastTouchTimestamp - rhs.lastTouchTimestamp
        if (lastTouchTimestampDelta == 0L) {
            // Use the standard compareTo method as a tie-break.
            lhs.compareTo(rhs)
        } else if (lhs.lastTouchTimestamp < rhs.lastTouchTimestamp) -1 else 1
    }

    init {
        require(maxCount >= 1) {
            "maxCount=$maxCount, you should not use Cache if you wanna cache nothing!"
        }
    }

    override fun requiresCacheSpanTouches(): Boolean = true

    override fun onCacheInitialized() {
        // Do nothing.
    }

    override fun onStartFile(cache: Cache, key: String, position: Long, length: Long) {
        if (length != C.LENGTH_UNSET.toLong()) {
            evictCache(cache)
        }
    }

    override fun onSpanAdded(cache: Cache, span: CacheSpan) {
        leastRecentlyUsed.add(span)
        currentCount++
        evictCache(cache)
    }

    override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
        leastRecentlyUsed.remove(span)
        currentCount--
    }

    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) {
        onSpanRemoved(cache, oldSpan)
        onSpanAdded(cache, newSpan)
    }

    private fun evictCache(cache: Cache) {
        while (currentCount > maxCount && !leastRecentlyUsed.isEmpty()) {
            cache.removeSpan(leastRecentlyUsed.first())
        }
    }
}
