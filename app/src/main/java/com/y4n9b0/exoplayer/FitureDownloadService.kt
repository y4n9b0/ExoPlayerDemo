package com.y4n9b0.exoplayer

import android.app.Notification
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.Scheduler

class FitureDownloadService : DownloadService(FOREGROUND_NOTIFICATION_ID_NONE) {
    override fun getDownloadManager(): DownloadManager {
        return FitureDownloadManager.newInstance(this).downloadManager
    }

    override fun getScheduler(): Scheduler? {
        // only start download service when host process is alive. see the doc of DownloadService#getScheduler
        return null
    }

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification {
        // this service runs in background, no need to show notification
        throw UnsupportedOperationException()
    }
}