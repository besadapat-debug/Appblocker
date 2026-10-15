package com.example.appblocker

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

object BlockOverlayManager {

    private var overlayView: View? = null
    private var tag: String? = null

    // What the overlay currently blocks, e.g. "app:com.instagram.android" or
    // "site:com.android.chrome".
    fun currentTag(): String? = if (overlayView != null) tag else null

    // Returns true if this is a NEW block (not just refreshing the same one),
    // so callers can count blocked attempts.
    fun show(context: Context, decision: BlockDecision, tag: String = "preview"): Boolean {
        val isNew = overlayView == null || this.tag != tag
        this.tag = tag

        val existing = overlayView
        if (existing != null) {
            bind(context, existing, decision)
            return isNew
        }

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = LayoutInflater.from(context).inflate(R.layout.blocked_overlay, null)
        bind(context, view, decision)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // A solid wall: intercept all touches (no FLAG_NOT_TOUCH_MODAL) and
            // stay opaque so the blocked app can't be seen or used underneath.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        )

        view.findViewById<Button>(R.id.goHomeButton).setOnClickListener {
            goHome(context)
            hide(context)
        }
        view.findViewById<Button>(R.id.pauseButton).setOnClickListener {
            if (PauseStore.startPause(context)) hide(context) else bind(context, view, decision)
        }
        view.findViewById<Button>(R.id.closeOverlayButton).setOnClickListener {
            hide(context)
        }

        try {
            windowManager.addView(view, params)
            overlayView = view
        } catch (e: Exception) {
            // Overlay permission missing - nothing we can draw.
            return false
        }
        return isNew
    }

    fun hide(context: Context) {
        val view = overlayView ?: return
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            windowManager.removeView(view)
        } catch (e: Exception) {
            // Already removed.
        }
        overlayView = null
        tag = null
    }

    private fun bind(context: Context, view: View, decision: BlockDecision) {
        view.findViewById<TextView>(R.id.overlayTitle).text = decision.title
        setOrHide(view.findViewById(R.id.overlaySubtitle), decision.subtitle)
        setOrHide(view.findViewById(R.id.overlayUntil), decision.untilText)
        setOrHide(view.findViewById(R.id.overlayMessage), AppSettings.blockMessage(context))

        val pause = view.findViewById<Button>(R.id.pauseButton)
        if (PauseStore.canPause(context)) {
            pause.visibility = View.VISIBLE
            pause.text = "Take a ${AppSettings.breakMinutes(context)}-min break " +
                "(${PauseStore.breaksLeft(context)} left today)"
        } else {
            pause.visibility = View.GONE
        }
    }

    private fun setOrHide(view: TextView, text: String?) {
        view.text = text ?: ""
        view.visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    private fun goHome(context: Context) {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(home)
        } catch (e: Exception) {
            // No launcher found.
        }
    }
}
