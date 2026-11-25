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
    private val h = Handler(Looper.getMainLooper())
    private var runningCleanup = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val prefs = getSharedPreferences("timer", MODE_PRIVATE)
        if (prefs.getBoolean("cleanupPending", false) && !runningCleanup) {
            runningCleanup = true
            prefs.edit().putBoolean("cleanupPending", false).apply()
            runCleanup()
        }
    }

    override fun onInterrupt() {}

    private fun runCleanup() {
        // Open Recents and close the task card—including Sleep Timer itself.
        performGlobalAction(GLOBAL_ACTION_RECENTS)
        h.postDelayed({ clickRecentsCloseButton(0) }, 1200)
        // Allow Recents to dismiss before opening Quick Settings.
        h.postDelayed({ performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) }, 3000)
        h.postDelayed({ toggleBluetoothTile() }, 4200)
        h.postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, 5200)
        h.postDelayed({ lockDevice() }, 6200)
    }

    private fun clickRecentsCloseButton(attempt: Int) {
        val labels = listOf("Close", "Close all", "Clear all", "CLEAR ALL", "清除全部", "সব বন্ধ করুন")
        val root = rootInActiveWindow
        val target = findLabel(root, labels)
        val clicked = target?.let { clickClickableAncestor(it) } ?: false
        if (!clicked && attempt < 2) {
            h.postDelayed({ clickRecentsCloseButton(attempt + 1) }, 650)
        }
    }

    private fun toggleBluetoothTile() {
        val root = rootInActiveWindow ?: return
        val nodes = collect(root)
        // OxygenOS often exposes the connected device name instead of a clear Bluetooth On/Off label.
        val candidates = nodes.filter { node ->
            val text = ((node.text?.toString() ?: "") + " " + (node.contentDescription?.toString() ?: "")).lowercase()
            text.contains("bluetooth") || text == "bt" || text.contains("rose ceramics")
        }
        val target = candidates.firstOrNull { it.isClickable } ?: candidates.firstOrNull()
        if (target != null) {
            // Timer is configured to turn Bluetooth OFF; the tile was expected to be ON when timer started.
            clickClickableAncestor(target)
        }
    }

    private fun findLabel(root: AccessibilityNodeInfo?, labels: List<String>): AccessibilityNodeInfo? {
        return collect(root).firstOrNull { node ->
            labels.any { label ->
                label.equals(node.text?.toString(), true) ||
                label.equals(node.contentDescription?.toString(), true)
            }
        }
    }

    private fun clickClickableAncestor(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 8) {
            if (current.isClickable && current.isEnabled) {
                if (current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            }
            current = current.parent
            depth++
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun collect(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        if (root == null) return emptyList()
        val out = mutableListOf<AccessibilityNodeInfo>()
        fun walk(node: AccessibilityNodeInfo) {
            out.add(node)
            for (i in 0 until node.childCount) node.getChild(i)?.let(::walk)
        }
        walk(root)
        return out
    }

    private fun lockDevice() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = ComponentName(this, LockDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(component)) dpm.lockNow() else performGlobalAction(GLOBAL_ACTION_HOME)
    }
}
