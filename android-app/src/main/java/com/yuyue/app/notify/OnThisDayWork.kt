package com.yuyue.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yuyue.app.MainActivity
import com.yuyue.app.R
import com.yuyue.app.YuyueApplication
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object OnThisDayWork {
    const val UNIQUE = "yuyue-on-this-day"
    const val CHANNEL_ID = "on_this_day"

    fun enqueue(context: Context) {
        ensureChannel(context)
        val delay = millisUntilNext(LocalTime.of(9, 0), ZoneId.of("Asia/Shanghai"))
        val req = PeriodicWorkRequestBuilder<OnThisDayWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE,
            ExistingPeriodicWorkPolicy.KEEP,
            req,
        )
    }

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.on_this_day_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        mgr.createNotificationChannel(channel)
    }

    fun millisUntilNext(at: LocalTime, zone: ZoneId, now: ZonedDateTime = ZonedDateTime.now(zone)): Long {
        var next = now.toLocalDate().atTime(at).atZone(zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis().coerceAtLeast(1L)
    }
}

class OnThisDayWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? YuyueApplication ?: return Result.retry()
        OnThisDayWork.ensureChannel(applicationContext)
        val zone = ZoneId.of("Asia/Shanghai")
        val today = LocalDate.now(zone)
        val nb = app.container.repository.defaultNotebookId() ?: return Result.success()
        val entries = app.container.repository.onThisDay(nb, today)
        val hits = entries.map {
            val year = java.time.Instant.ofEpochMilli(it.startAtMillis).atZone(zone).year
            OnThisDayNotify.Hit(year, it.title.ifBlank { it.rawTranscript?.take(24).orEmpty() })
        }
        val content = OnThisDayNotify.build(hits) ?: return Result.success()
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_ON_THIS_DAY, true)
        }
        val pending = PendingIntent.getActivity(
            applicationContext,
            71,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, OnThisDayWork.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFY_ID, notification)
        }
        return Result.success()
    }

    companion object {
        const val NOTIFY_ID = 7101
        const val EXTRA_OPEN_ON_THIS_DAY = "open_on_this_day"
    }
}
