package com.y4n9b0.exoplayer

import android.util.LruCache
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheEvictor
import com.google.android.exoplayer2.upstream.cache.CacheSpan
import java.util.TreeSet

/**
 * maxSize - 最大视频个数（而非视频大小 size）
 * 视频大小 Size 逐出策略请使用 [com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor]
 */
class LruCacheEvictor(private val maxSize: Int) : CacheEvictor {

    private val leastRecentlyUsed: CacheSpanLruCache = CacheSpanLruCache(maxSize)
    private val comparator: Comparator<CacheSpan> by lazy {
        Comparator { lhs, rhs ->
            val lastTouchTimestampDelta: Long = lhs.lastTouchTimestamp - rhs.lastTouchTimestamp
            if (lastTouchTimestampDelta == 0L) {
                // Use the standard compareTo method as a tie-break.
                lhs.compareTo(rhs)
            } else if (lhs.lastTouchTimestamp < rhs.lastTouchTimestamp) -1 else 1
        }
    }

    init {
        require(maxSize >= 1) {
            "maxSize=$maxSize, should not use Cache if you wanna cache nothing!"
        }
    }

    override fun requiresCacheSpanTouches(): Boolean = true

    override fun onCacheInitialized() {
        // Do nothing.
    }

    override fun onStartFile(cache: Cache, key: String, position: Long, length: Long) {
        // Do nothing.
    }

    override fun onSpanAdded(cache: Cache, span: CacheSpan) {
        leastRecentlyUsed.cache = cache
        val cacheSpanTreeSet: TreeSet<CacheSpan> = leastRecentlyUsed.get(span.key) ?: TreeSet(comparator)
        cacheSpanTreeSet.add(span)
        leastRecentlyUsed.put(span.key, cacheSpanTreeSet)
    }

    override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
        leastRecentlyUsed.cache = cache
        val cacheSpanTreeSet: TreeSet<CacheSpan>? = leastRecentlyUsed.get(span.key)
        cacheSpanTreeSet?.apply {
            remove(span)
            if (isEmpty()) {
                leastRecentlyUsed.remove(span.key)
            }
        }
    }

    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) {
        onSpanRemoved(cache, oldSpan)
        onSpanAdded(cache, newSpan)
    }
}

private class CacheSpanLruCache(maxSize: Int) : LruCache<String, TreeSet<CacheSpan>>(maxSize) {
    lateinit var cache: Cache

    override fun entryRemoved(
        evicted: Boolean,
        key: String?,
        oldValue: TreeSet<CacheSpan>?,
        newValue: TreeSet<CacheSpan>?
    ) {
        if (evicted) {
            oldValue?.onEach(cache::removeSpan)
        }
    }
}
