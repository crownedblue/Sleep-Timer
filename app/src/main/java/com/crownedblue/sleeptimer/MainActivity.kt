package com.crownedblue.sleeptimer

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SleepTimerScreen(this) }
    }
}

private val BG = Color(0xFF080A0F)
private val Glass = Color(0x18FFFFFF)
private val Border = Color(0x30FFFFFF)
private val Accent = Color(0xFF8EAED8)
private val AccentDeep = Color(0xFF243B58)
private val AccentGlass = Color(0x553C5F88

@Composable
fun SleepTimerScreen(activity: Activity) {
    val prefs = activity.getSharedPreferences("timer", Context.MODE_PRIVATE)
    var selected by remember { mutableFloatStateOf((prefs.getInt("durationSeconds", prefs.getInt("minutes", 30) * 60) / 60f).coerceIn(0f, 120f)) }
    var active by remember { mutableStateOf(prefs.getBoolean("running", false)) }
    var remaining by remember { mutableLongStateOf(0L) }
    var paused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val end = prefs.getLong("endAt", 0L)
            remaining = if (prefs.getBoolean("running", false) && end > 0) ((end - System.currentTimeMillis()).coerceAtLeast(0) / 1000) else 0
            active = prefs.getBoolean("running", false)
            paused = prefs.getBoolean("paused", false)
            delay(250)
        }
    }

    val adminLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Box(Modifier.fillMaxSize().background(BG)) {
        Box(Modifier.size(280.dp).align(Alignment.TopCenter).offset(y = (-100).dp).blur(100.dp).background(Color(0xFF456D9C).copy(alpha=.22f), CircleShape))
        Box(Modifier.size(240.dp).align(Alignment.BottomEnd).offset(x=90.dp,y=80.dp).blur(90.dp).background(Color(0xFF5D70A5).copy(alpha=.15f), CircleShape))
        Column(Modifier.fillMaxSize().padding(horizontal=26.dp, vertical=22.dp), horizontalAlignment=Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SLEEP TIMER", color=Color.White, fontSize=12.sp, fontWeight=FontWeight.SemiBold, letterSpacing=3.sp)
                    Text("Quietly, on your terms.", color=Color.White.copy(.48f), fontSize=12.sp)
                }
                Box(Modifier.size(40.dp).clip(CircleShape).background(Glass).border(1.dp, Border, CircleShape), contentAlignment=Alignment.Center) {
                    Text("✦", color=Accent, fontSize=16.sp)
                }
            }
            Spacer(Modifier.weight(.14f))
            Box(Modifier.size(280.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0x1E9CBDE5), Color(0x08FFFFFF), Color.Transparent))).border(1.dp, Color.White.copy(.10f), CircleShape), contentAlignment=Alignment.Center) {
                Box(Modifier.size(245.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0x16FFFFFF), Color(0x04FFFFFF), Color(0x103C6798)))).border(1.dp, Color.White.copy(.13f), CircleShape), contentAlignment=Alignment.Center) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(if (active && !paused) "REMAINING" else "DURATION", color=Color.White.copy(.45f), letterSpacing=2.4.sp, fontSize=10.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(formatTime(if (active) remaining else if(selected.toInt()==0) 10L else selected.toLong()*60), color=Color.White, fontSize=54.sp, fontWeight=FontWeight.Light, letterSpacing=(-2).sp)
                        Spacer(Modifier.height(7.dp))
                        Text(if(active) if(paused) "PAUSED" else "COUNTING DOWN" else if(selected.toInt()==0) "SECONDS" else "MINUTES", color=Accent.copy(.75f), fontSize=10.sp, letterSpacing=2.sp)
                    }
                }
            }
            Spacer(Modifier.height(34.dp))
            GlassPanel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                    Text("SET DURATION", color=Color.White.copy(.65f), fontSize=11.sp, letterSpacing=1.6.sp)
                    Text(if(selected.toInt()==0) "10 sec" else "${selected.toInt()} min", color=Accent, fontSize=14.sp, fontWeight=FontWeight.Medium)
                }
                Slider(value=selected, onValueChange={ if(!active) selected=it }, valueRange=0f..120f, steps=119, enabled=!active, colors=SliderDefaults.colors(thumbColor=Accent, activeTrackColor=Accent, inactiveTrackColor=Color.White.copy(.12f), activeTickColor=AccentDeep, inactiveTickColor=Color.White.copy(.18f)))
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) { Text("10 sec", color=Color.White.copy(.35f), fontSize=10.sp); Text("60 min", color=Color.White.copy(.35f), fontSize=10.sp); Text("2 hr", color=Color.White.copy(.35f), fontSize=10.sp) }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Button(onClick={
                    if (!active) {
                        val durationSeconds = if(selected.toInt()==0) 10 else selected.toInt()*60\n                        prefs.edit().putInt("minutes", if(selected.toInt()==0) 1 else selected.toInt()).putInt("durationSeconds", durationSeconds).putBoolean("running", true).putBoolean("paused", false).putLong("endAt",System.currentTimeMillis()+durationSeconds*1000L).apply()
                        val intent=Intent(activity,TimerService::class.java).setAction(TimerService.ACTION_START)
                        if(Build.VERSION.SDK_INT>=26) activity.startForegroundService(intent) else activity.startService(intent)
                        active=true
                    } else if(paused) {
                        prefs.edit().putBoolean("paused",false).putLong("endAt",System.currentTimeMillis()+prefs.getLong("remaining",0)).apply()
                        activity.startService(Intent(activity,TimerService::class.java).setAction(TimerService.ACTION_RESUME))
                    } else {
                        prefs.edit().putBoolean("paused",true).putLong("remaining",remaining*1000).apply()
                        activity.startService(Intent(activity,TimerService::class.java).setAction(TimerService.ACTION_PAUSE))
                    }
                }, modifier=Modifier.weight(1f).height(58.dp), shape=RoundedCornerShape(20.dp), colors=ButtonDefaults.buttonColors(containerColor=AccentDeep, contentColor=Color(0xFFEAF2FC))) {
                    Icon(if(active&&!paused) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp)); Text(if(!active) "START TIMER" else if(paused) "RESUME" else "PAUSE", fontWeight=FontWeight.SemiBold, letterSpacing=1.sp)
                }
                if(active) OutlinedButton(onClick={activity.startService(Intent(activity,TimerService::class.java).setAction(TimerService.ACTION_CANCEL));prefs.edit().putBoolean("running",false).putBoolean("paused",false).apply();active=false;remaining=0}, modifier=Modifier.height(58.dp), shape=RoundedCornerShape(20.dp), colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.White), border=androidx.compose.foundation.BorderStroke(1.dp, Border)) { Icon(Icons.Default.Close,null) }
            }
            Spacer(Modifier.height(20.dp))
            GlassPanel {
                Text("WHEN TIME IS UP", color=Color.White.copy(.6f), fontSize=10.sp, letterSpacing=1.8.sp)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.Bluetooth,null,tint=Accent,modifier=Modifier.size(18.dp)); Spacer(Modifier.width(10.dp)); Text("Bluetooth off",color=Color.White,fontSize=13.sp); Spacer(Modifier.weight(1f)); Text("↘",color=Accent.copy(.7f))
                }
                Spacer(Modifier.height(12.dp)); HorizontalDivider(color=Color.White.copy(.08f)); Spacer(Modifier.height(12.dp))
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock,null,tint=Accent,modifier=Modifier.size(18.dp)); Spacer(Modifier.width(10.dp)); Text("Clear recent apps & lock",color=Color.White,fontSize=13.sp); Spacer(Modifier.weight(1f)); Text("↘",color=Accent.copy(.7f))
                }
            }
            Spacer(Modifier.weight(.15f))
            Text("FIRST-TIME SETUP", color=Color.White.copy(.35f), fontSize=9.sp, letterSpacing=1.5.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                TextButton(onClick={ activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("Accessibility", color=Accent.copy(.8f), fontSize=11.sp) }
                TextButton(onClick={ val cn=ComponentName(activity,LockDeviceAdminReceiver::class.java); val req=Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,cn).putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,"Sleep Timer needs permission to lock your screen when the countdown ends."); adminLauncher.launch(req) }) { Text("Screen lock", color=Accent.copy(.8f), fontSize=11.sp) }
            }
            if(Build.VERSION.SDK_INT>=33) TextButton(onClick={notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)}) { Text("Allow notifications", color=Color.White.copy(.55f), fontSize=10.sp) }
            Text("One setup. Then just press play.", color=Color.White.copy(.25f), fontSize=10.sp)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable fun GlassPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0x1DFFFFFF),Color(0x0AFFFFFF)))).border(1.dp,Border,RoundedCornerShape(24.dp)).padding(18.dp), content=content)
}

fun formatTime(seconds: Long): String = "%02d:%02d".format(seconds/60,seconds%60)
