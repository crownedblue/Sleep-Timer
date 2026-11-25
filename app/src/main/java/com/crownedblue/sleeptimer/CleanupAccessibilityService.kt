package com.crownedblue.sleeptimer

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent

class CleanupAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var cleanupRunning = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        checkPendingCleanup()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = checkPendingCleanup()

    private fun checkPendingCleanup() {
        val prefs = getSharedPreferences("timer", MODE_PRIVATE)
        if (prefs.getBoolean("cleanupPending", false) && !cleanupRunning) {
            cleanupRunning = true
            prefs.edit().putBoolean("cleanupPending", false).apply()
            runCleanup()
        }
    }

    override fun onInterrupt() {}

    private fun runCleanup() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
        handler.postDelayed({ clickRecentsCloseButton(0) }, 1200)
        handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) }, 3000)
        handler.postDelayed({ toggleBluetoothTile() }, 4200)
        handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, 5200)
        handler.postDelayed({ lockDevice() }, 6200)
        // Release guard for the next timer run.
        handler.postDelayed({
            cleanupRunning = false
            checkPendingCleanup()
        }, 8000)
    }

    private fun clickRecentsCloseButton(attempt: Int) {
        val labels = listOf("Close", "Close all", "Clear all", "CLEAR ALL", "清除全部", "সব বন্ধ করুন")
        val target = findLabel(rootInActiveWindow, labels)
        val clicked = target?.let { clickClickableAncestor(it) } ?: false
        if (!clicked && attempt < 3) handler.postDelayed({ clickRecentsCloseButton(attempt + 1) }, 700)
    }

    private fun toggleBluetoothTile() {
        val nodes = collect(rootInActiveWindow)
        val candidates = nodes.filter { node ->
            val text = ((node.text?.toString() ?: "") + " " + (node.contentDescription?.toString() ?: "")).lowercase()
            text.contains("bluetooth") || text == "bt" || text.contains("rose ceramics")
        }
        val target = candidates.firstOrNull { it.isClickable && it.isEnabled } ?: candidates.firstOrNull()
        target?.let { clickClickableAncestor(it) }
    }

    private fun findLabel(root: AccessibilityNodeInfo?, labels: List<String>): AccessibilityNodeInfo? =
        collect(root).firstOrNull { node -> labels.any { label ->
            label.equals(node.text?.toString(), true) || label.equals(node.contentDescription?.toString(), true)
        } }

    private fun clickClickableAncestor(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 8) {
            if (current.isClickable && current.isEnabled && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            current = current.parent
            depth++
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun collect(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        if (root == null) return emptyList()
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            result.add(node)
            for (i in 0 until node.childCount) node.getChild(i)?.let(::visit)
        }
        visit(root)
        return result
    }

    private fun lockDevice() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, LockDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(admin)) dpm.lockNow() else performGlobalAction(GLOBAL_ACTION_HOME)
    }
}
