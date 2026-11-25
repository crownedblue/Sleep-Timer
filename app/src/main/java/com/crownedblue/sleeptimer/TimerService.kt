package com.crownedblue.sleeptimer

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class TimerService : Service() {
    private val channel = "sleep_timer"
    private val handler = Handler(Looper.getMainLooper())

    private val tick = object : Runnable {
        override fun run() {
            val prefs = getSharedPreferences("timer", MODE_PRIVATE)
            if (!prefs.getBoolean("running", false) || prefs.getBoolean("paused", false)) return
            val remaining = prefs.getLong("endAt", 0L) - System.currentTimeMillis()
            if (remaining <= 0L) {
                finishTimer()
                return
            }
            val minutes = (remaining / 60000).toInt()
            val seconds = ((remaining % 60000) / 1000).toInt()
            getSystemService(NotificationManager::class.java).notify(
                1, notification("Timer running", String.format("%02d:%02d remaining", minutes, seconds))
            )
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("timer", MODE_PRIVATE)
        when (intent?.action) {
            ACTION_START, ACTION_RESUME -> {
                handler.removeCallbacks(tick)
                val end = prefs.getLong("endAt", 0L)
                startForeground(1, notification("Timer running", "Finishes at ${java.text.SimpleDateFormat("h:mm a").format(end)}"))
                handler.post(tick)
            }
            ACTION_PAUSE -> {
                handler.removeCallbacks(tick)
                prefs.edit().putBoolean("paused", true).apply()
                startForeground(1, notification("Timer paused", "Tap Sleep Timer to resume"))
            }
            ACTION_CANCEL -> {
                handler.removeCallbacks(tick)
                prefs.edit().putBoolean("running", false).putBoolean("paused", false)
                    .putBoolean("cleanupPending", false).putLong("endAt", 0L).apply()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun finishTimer() {
        val prefs = getSharedPreferences("timer", MODE_PRIVATE)
        prefs.edit().putBoolean("running", false).putBoolean("paused", false)
            .putBoolean("cleanupPending", true).putLong("endAt", 0L).apply()
        getSystemService(NotificationManager::class.java).notify(
            2, NotificationCompat.Builder(this, channel)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Sleep Timer finished")
                .setContentText("Starting cleanup and locking device")
                .setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH).build()
        )
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (launch != null) startActivity(launch)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(title: String, text: String): Notification {
        val pending = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channel).setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title).setContentText(text).setContentIntent(pending)
            .setOngoing(title == "Timer running" || title == "Timer paused")
            .setOnlyAlertOnce(true).setPriority(NotificationCompat.PRIORITY_LOW).build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val notificationChannel = NotificationChannel(channel, "Sleep Timer", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(notificationChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "start"
        const val ACTION_PAUSE = "pause"
        const val ACTION_RESUME = "resume"
        const val ACTION_CANCEL = "cancel"
    }
}
