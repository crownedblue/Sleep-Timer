package com.crownedblue.sleeptimer

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class CleanupAccessibilityService : AccessibilityService() {
    private val h=Handler(Looper.getMainLooper())
    private var runningCleanup=false
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val p=getSharedPreferences("timer",MODE_PRIVATE)
        if(p.getBoolean("cleanupPending",false) && !runningCleanup){ runningCleanup=true; p.edit().putBoolean("cleanupPending",false).apply(); runCleanup() }
    }
    override fun onInterrupt() {}
    private fun runCleanup(){
        performGlobalAction(GLOBAL_ACTION_RECENTS)
        h.postDelayed({ tapText(rootInActiveWindow,listOf("Clear all","Close all","Close","CLEAR ALL","清除全部","সব বন্ধ করুন")) },1300)
        h.postDelayed({ performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) },2400)
        h.postDelayed({ tapBluetoothTile(rootInActiveWindow) },3400)
        h.postDelayed({ lockDevice() },5000)
    }
    private fun tapBluetoothTile(root:AccessibilityNodeInfo?) {
        if(root==null)return
        val labels=listOf("Bluetooth","Bluetooth off","Bluetooth on","BT")
        val nodes=collect(root)
        val target=nodes.firstOrNull { n -> labels.any { it.equals(n.text?.toString(),true) || it.equals(n.contentDescription?.toString(),true) } }
        if(target!=null){
            val desc=(target.contentDescription?.toString()+" "+target.text?.toString()).lowercase()
            if(desc.contains("on") || target.isChecked || target.isSelected) target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        h.postDelayed({performGlobalAction(GLOBAL_ACTION_BACK)},500)
    }
    private fun tapText(root:AccessibilityNodeInfo?, labels:List<String>){
        val n=collect(root).firstOrNull { node -> labels.any { it.equals(node.text?.toString(),true) || it.equals(node.contentDescription?.toString(),true) } }
        n?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
    private fun collect(root:AccessibilityNodeInfo?):List<AccessibilityNodeInfo>{
        if(root==null)return emptyList(); val out=mutableListOf<AccessibilityNodeInfo>(); fun walk(n:AccessibilityNodeInfo){out.add(n);for(i in 0 until n.childCount)n.getChild(i)?.let(::walk)};walk(root);return out
    }
    private fun lockDevice(){
        val dpm=getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val cn=ComponentName(this,LockDeviceAdminReceiver::class.java)
        if(dpm.isAdminActive(cn)){dpm.lockNow()} else {performGlobalAction(GLOBAL_ACTION_HOME)}
    }
}