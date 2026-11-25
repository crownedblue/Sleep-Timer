package com.crownedblue.sleeptimer

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class TimerService : Service() {
    private val channel = "sleep_timer"
    override fun onCreate() { super.onCreate(); createChannel() }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_START, ACTION_RESUME -> {
                val end = getSharedPreferences("timer", MODE_PRIVATE).getLong("endAt",0)
                startForeground(1, notification("Timer running", "Finishes at ${java.text.SimpleDateFormat("h:mm a").format(end)}"))
                Thread {
                    while(getSharedPreferences("timer", MODE_PRIVATE).getBoolean("running",false) && !getSharedPreferences("timer", MODE_PRIVATE).getBoolean("paused",false)) {
                        val remaining=(getSharedPreferences("timer", MODE_PRIVATE).getLong("endAt",0)-System.currentTimeMillis())
                        if(remaining<=0){ finishTimer(); break }
                        val min=(remaining/60000).toInt(); val sec=((remaining%60000)/1000).toInt()
                        val nm=getSystemService(NotificationManager::class.java)
                        nm.notify(1,notification("Timer running",String.format("%02d:%02d remaining",min,sec)))
                        try{Thread.sleep(1000)}catch(_:Exception){}
                    }
                }.start()
            }
            ACTION_PAUSE -> { getSharedPreferences("timer", MODE_PRIVATE).edit().putBoolean("paused",true).apply(); startForeground(1,notification("Timer paused","Tap Sleep Timer to resume")) }
            ACTION_CANCEL -> { getSharedPreferences("timer", MODE_PRIVATE).edit().putBoolean("running",false).putBoolean("paused",false).putLong("endAt",0).apply(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
        }
        return START_NOT_STICKY
    }
    private fun finishTimer(){
        val p=getSharedPreferences("timer", MODE_PRIVATE)
        p.edit().putBoolean("running",false).putBoolean("paused",false).putBoolean("cleanupPending",true).putLong("endAt",0).apply()
        val nm=getSystemService(NotificationManager::class.java)
        nm.notify(2,NotificationCompat.Builder(this,channel).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Sleep Timer finished").setContentText("Starting cleanup and locking device").setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH).build())
        val launch=packageManager.getLaunchIntentForPackage(packageName)
        launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if(launch!=null) startActivity(launch)
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }
    private fun notification(title:String,text:String):Notification {
        val pi=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this,channel).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(title).setContentText(text).setContentIntent(pi).setOngoing(true).setOnlyAlertOnce(true).setPriority(NotificationCompat.PRIORITY_LOW).build()
    }
    private fun createChannel(){ if(Build.VERSION.SDK_INT>=26){ val c=NotificationChannel(channel,"Sleep Timer",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager::class.java).createNotificationChannel(c) } }
    override fun onBind(intent:Intent?):IBinder?=null
    companion object { const val ACTION_START="start"; const val ACTION_PAUSE="pause"; const val ACTION_RESUME="resume"; const val ACTION_CANCEL="cancel" }
}