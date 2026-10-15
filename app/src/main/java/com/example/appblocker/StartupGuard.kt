package com.example.appblocker

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView

// After a restart (or an app update), covers the whole screen until the
// blocker is confirmed running AND a minimum wait has passed, so nothing
// can be opened in the gap before blocking starts.
object StartupGuard {

    private const val GIVE_UP_MS = 90_000L

    private val handler = Handler(Looper.getMainLooper())
    private var view: View? = null
    private var shownAt = 0L

    // Only worth locking the phone if there's something to protect.
    fun isNeeded(context: Context): Boolean =
        StrictModeStore.isActive(context) ||
            QuickBlockStore.isRunning(context) ||
            BlockListStore.userLists(context).any { it.enabled }

    fun show(context: Context) {
        val app = context.applicationContext
        if (view != null) return
        if (!Settings.canDrawOverlays(app)) return

        val v = LayoutInflater.from(app).inflate(R.layout.startup_guard, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        v.findViewById<Button>(R.id.guardSettingsButton).setOnClickListener {
            hide(app)
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                app.startActivity(intent)
            } catch (e: Exception) {
                // settings unavailable
            }
        }
        try {
            (app.getSystemService(Context.WINDOW_SERVICE) as WindowManager).addView(v, params)
        } catch (e: Exception) {
            return
        }
        view = v
        shownAt = System.currentTimeMillis()
        handler.post(object : Runnable {
            override fun run() {
                if (view == null) return
                if (update(app)) handler.postDelayed(this, 500)
            }
        })
    }

    fun hide(context: Context) {
        val v = view ?: return
        try {
            (context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(v)
        } catch (e: Exception) {
            // already gone
        }
        view = null
    }

    // Returns false once finished (hidden or showing the fallback).
    private fun update(context: Context): Boolean {
        val v = view ?: return false
        val elapsed = System.currentTimeMillis() - shownAt
        val minHold = AppSettings.startupLockSeconds(context) * 1000L
        val running = AppBlockAccessibilityService.isRunning()
        val permissionOn = PermissionUtils.hasAccessibilityPermission(context)

        if (running && elapsed >= minHold) {
            hide(context)
            AppBlockAccessibilityService.recheckNow()
            return false
        }

        if ((!permissionOn && elapsed >= minHold) || elapsed >= GIVE_UP_MS) {
            // Blocking can't start - say so rather than locking the phone forever.
            v.findViewById<TextView>(R.id.guardTitle).text = "Blocking isn't running"
            v.findViewById<TextView>(R.id.guardText).text =
                "AppBlocker's Accessibility access is off, so your blocks can't work. Turn it back on to continue."
            v.findViewById<TextView>(R.id.guardCountdown).visibility = View.GONE
            v.findViewById<ProgressBar>(R.id.guardProgress).visibility = View.GONE
            v.findViewById<Button>(R.id.guardSettingsButton).visibility = View.VISIBLE
            return false
        }

        val secondsLeft = ((minHold - elapsed + 999) / 1000).coerceAtLeast(0)
        v.findViewById<TextView>(R.id.guardCountdown).text = when {
            !running -> "Starting blocker…"
            else -> "Ready in ${secondsLeft}s"
        }
        return true
    }
}
