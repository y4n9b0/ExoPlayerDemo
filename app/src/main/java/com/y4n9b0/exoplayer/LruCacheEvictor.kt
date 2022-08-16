package com.y4n9b0.exoplayer

import android.util.LruCache
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheEvictor
import com.google.android.exoplayer2.upstream.cache.CacheSpan
import java.util.*

/**
 * @param maxBytes 最大缓存空间
 * @param maxMediaCount 最多缓存媒体文件个数 fixme: m3u8
 */
class LruCacheEvictor(private val maxBytes: Long, private val maxMediaCount: Int) :
    LruCache<String, TreeSet<CacheSpan>>(maxMediaCount), CacheEvictor {

    private lateinit var cache: Cache
    private var currentSize: Long = 0

    override fun requiresCacheSpanTouches() = true

    override fun onCacheInitialized() {
        // Do nothing.
    }

    override fun onStartFile(cache: Cache, key: String, position: Long, length: Long) {
        this.cache = cache
        if (length != C.LENGTH_UNSET.toLong()) {
            evictCache(length)
        }
    }

    override fun onSpanAdded(cache: Cache, span: CacheSpan) {
        this.cache = cache
        val treeSet: TreeSet<CacheSpan> = get(span.key) ?: TreeSet(::compare)
        treeSet.add(span)
        put(span.key, treeSet)
        currentSize += span.length
        evictCache(0)
    }

    override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
        this.cache = cache
        val treeSet: TreeSet<CacheSpan>? = get(span.key)
        treeSet?.also {
            it.remove(span)
            if (it.isEmpty()) remove(span.key)
        }
        currentSize -= span.length
    }

    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) {
        onSpanRemoved(cache, oldSpan)
        onSpanAdded(cache, newSpan)
    }

    override fun entryRemoved(
        evicted: Boolean,
        key: String?,
        oldValue: TreeSet<CacheSpan>?,
        newValue: TreeSet<CacheSpan>?
    ) {
        if (evicted && oldValue != null) {
            oldValue.forEach(cache::removeSpan)
        }
    }

    private fun evictCache(requiredSpace: Long) {
        while ((currentSize + requiredSpace > maxBytes && size() > 0) || size() > maxMediaCount) {
            trimToSize(size() - 1)
        }
    }

    private fun compare(lhs: CacheSpan, rhs: CacheSpan): Int {
        val lastTouchTimestampDelta = lhs.lastTouchTimestamp - rhs.lastTouchTimestamp
        if (lastTouchTimestampDelta == 0L) {
            // Use the standard compareTo method as a tie-break.
            return lhs.compareTo(rhs)
        }
        return if (lhs.lastTouchTimestamp < rhs.lastTouchTimestamp) -1 else 1
    }
}
