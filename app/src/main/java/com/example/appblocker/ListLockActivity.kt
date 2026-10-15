package com.example.appblocker

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.concurrent.TimeUnit

/**
 * An independent Strict lock for one block list (or the adult filter). It uses
 * the SAME unlock engine as the master lock, but scoped to this item, so
 * unlocking it doesn't touch any other lock. While it's on, the item can't be
 * disabled/weakened and the app can't be uninstalled.
 */
class ListLockActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SCOPE = "scope"
        const val EXTRA_TITLE = "title"
    }

    private lateinit var scope: String
    private lateinit var titleText: String
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var statusView: TextView
    private lateinit var timerView: TextView
    private lateinit var methodButton: TextView
    private lateinit var toggleButton: TextView
    private lateinit var cooldownView: TextView

    private var pinVerified = false
    private var ticker: Runnable? = null
    private var clockTicker: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scope = intent.getStringExtra(EXTRA_SCOPE) ?: ListLocks.ADULT_SCOPE
        titleText = intent.getStringExtra(EXTRA_TITLE) ?: "This item"
        setContentView(buildUi())
        renderState()
    }

    override fun onResume() {
        super.onResume()
        renderState()
        startClock()
    }

    override fun onPause() {
        super.onPause()
        ticker?.let { handler.removeCallbacks(it) }
        clockTicker?.let { handler.removeCallbacks(it) }
    }

    // ----- UI (built in code to stay self-contained) -----

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@ListLockActivity, R.color.bg_dark))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }

        root.addView(TextView(this).apply {
            text = "Lock: $titleText"
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "A separate Strict lock just for this. Unlocking it won't unlock anything else, and the app can't be uninstalled while any lock is on."
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(8), 0, dp(20))
        })

        statusView = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(statusView)

        timerView = TextView(this).apply {
            setTextColor(ContextCompat.getColor(context, R.color.accent_blue))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setPadding(0, 0, 0, dp(12))
            visibility = View.GONE
        }
        root.addView(timerView)

        methodButton = card("Unlock method") { openMethods() }
        root.addView(methodButton)

        cooldownView = TextView(this).apply {
            setTextColor(ContextCompat.getColor(context, R.color.accent_blue))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, dp(12), 0, 0)
            visibility = View.GONE
        }
        root.addView(cooldownView)

        toggleButton = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setBackgroundResource(R.drawable.bg_time_button)
            setPadding(0, dp(16), 0, dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(20) }
            setOnClickListener { onToggle() }
        }
        root.addView(toggleButton)

        root.addView(TextView(this).apply {
            text = "Close"
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setPadding(0, dp(20), 0, 0)
            setOnClickListener { finish() }
        })

        scroll.addView(root)
        return scroll
    }

    private fun card(label: String, onClick: () -> Unit): TextView = TextView(this).apply {
        text = label
        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setBackgroundResource(R.drawable.bg_range_card)
        setPadding(dp(16), dp(16), dp(16), dp(16))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        setOnClickListener { onClick() }
    }

    private fun openMethods() {
        if (StrictModeStore.isActive(this, scope)) {
            Toast.makeText(this, "Unlock methods are locked while this is locked", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            android.content.Intent(this, UnlockMethodActivity::class.java)
                .putExtra(UnlockMethodActivity.EXTRA_SCOPE, scope)
        )
    }

    // ----- Activate / deactivate (scoped copy of the master flow) -----

    private fun onToggle() {
        if (!StrictModeStore.isActive(this, scope)) {
            StrictModeStore.setActive(this, true, scope)
            UnlockMethodStore.recordActivatedNow(this, scope)
            pinVerified = false
            renderState()
            return
        }
        pinVerified = false
        attemptDeactivate()
    }

    private fun attemptDeactivate() {
        val selected = UnlockMethodStore.getSelectedMethods(this, scope)

        if (selected.isEmpty()) { finalizeDeactivate(); return }

        if (UnlockMethod.PIN_CODE in selected && !pinVerified) {
            promptForPin(); return
        }

        if (UnlockMethod.COOLDOWN in selected) {
            val startedAt = UnlockMethodStore.getCooldownStartedAt(this, scope)
            val requiredMs = TimeUnit.SECONDS.toMillis(UnlockMethodStore.getCooldownSeconds(this, scope).toLong())
            if (startedAt == 0L) { UnlockMethodStore.startCooldown(this, scope); tickCooldown(); return }
            if (requiredMs - (System.currentTimeMillis() - startedAt) > 0) { tickCooldown(); return }
        }

        val unmet = UnlockChecker.checkAmbientMethods(this, scope).filter { !it.satisfied }
        if (unmet.isNotEmpty()) {
            Toast.makeText(this, unmet.joinToString(" · ") { it.reason }, Toast.LENGTH_LONG).show()
            return
        }
        finalizeDeactivate()
    }

    private fun tickCooldown() {
        val startedAt = UnlockMethodStore.getCooldownStartedAt(this, scope)
        val requiredMs = TimeUnit.SECONDS.toMillis(UnlockMethodStore.getCooldownSeconds(this, scope).toLong())
        val remainingMs = requiredMs - (System.currentTimeMillis() - startedAt)
        if (remainingMs <= 0) {
            cooldownView.visibility = View.GONE
            attemptDeactivate()
            return
        }
        cooldownView.visibility = View.VISIBLE
        cooldownView.text = "Unlocking in ${(remainingMs / 1000) + 1}s…"
        val r = Runnable { tickCooldown() }
        ticker = r
        handler.postDelayed(r, 1000)
    }

    private fun finalizeDeactivate() {
        StrictModeStore.setActive(this, false, scope)
        UnlockMethodStore.clearCooldown(this, scope)
        pinVerified = false
        cooldownView.visibility = View.GONE
        Toast.makeText(this, "Unlocked", Toast.LENGTH_SHORT).show()
        renderState()
    }

    private fun promptForPin() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_enter_pin, null)
        val input = dialogView.findViewById<EditText>(R.id.pinInput)
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Unlock") { _, _ ->
                if (UnlockMethodStore.checkPin(this, input.text.toString(), scope)) {
                    pinVerified = true
                    attemptDeactivate()
                } else {
                    Toast.makeText(this, "Wrong PIN", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ----- Rendering -----

    private fun startClock() {
        clockTicker?.let { handler.removeCallbacks(it) }
        val r = object : Runnable {
            override fun run() {
                renderTimer()
                handler.postDelayed(this, 1000)
            }
        }
        clockTicker = r
        handler.post(r)
    }

    private fun renderState() {
        val active = StrictModeStore.isActive(this, scope)
        statusView.text = if (active) "Locked" else "Unlocked"
        statusView.setTextColor(
            ContextCompat.getColor(this, if (active) R.color.accent_green else R.color.text_secondary)
        )
        toggleButton.text = if (active) "Deactivate this lock" else "Activate this lock"
        methodButton.visibility = if (active) View.GONE else View.VISIBLE
        val selected = UnlockMethodStore.getSelectedMethods(this, scope)
        methodButton.text = if (selected.isEmpty()) "Unlock method: none (tap to choose)"
        else "Unlock method: " + selected.joinToString(" + ") { it.label }
        renderTimer()
    }

    private fun renderTimer() {
        val active = StrictModeStore.isActive(this, scope)
        if (!active || UnlockMethod.TIMER !in UnlockMethodStore.getSelectedMethods(this, scope)) {
            timerView.visibility = View.GONE
            return
        }
        val activatedAt = UnlockMethodStore.getActivatedAt(this, scope)
        val totalMs = TimeUnit.MINUTES.toMillis(UnlockMethodStore.getTimerMinutes(this, scope).toLong())
        val remainingMs = (activatedAt + totalMs) - System.currentTimeMillis()
        timerView.visibility = View.VISIBLE
        timerView.text = if (remainingMs <= 0) "Timer finished — you can unlock now"
        else "Unlocks in ${formatClock(remainingMs)}"
    }

    private fun formatClock(ms: Long): String {
        val total = (ms / 1000) + if (ms % 1000 > 0) 1 else 0
        val h = total / 3600; val m = (total % 3600) / 60; val s = total % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()
}
