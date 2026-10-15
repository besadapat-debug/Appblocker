package com.example.appblocker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

// Pick two apps to open side by side, or one-tap a saved combo. Needed because
// Strict Mode can block Recent apps, the normal way to make a split.
class SplitLauncherActivity : AppCompatActivity() {

    private var topApp: AppInfo? = null
    private var bottomApp: AppInfo? = null
    private val apps: List<AppInfo> by lazy { AppCatalog.launcherApps(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_split_launcher)

        findViewById<ImageButton>(R.id.closeButton).setOnClickListener { finish() }
        findViewById<View>(R.id.topSlot).setOnClickListener { pickApp(isTop = true) }
        findViewById<View>(R.id.bottomSlot).setOnClickListener { pickApp(isTop = false) }
        findViewById<MaterialButton>(R.id.launchSplitButton).setOnClickListener { launchCurrent() }
        findViewById<MaterialButton>(R.id.saveComboButton).setOnClickListener { saveCurrent() }
        render()
    }

    override fun onResume() {
        super.onResume()
        renderCombos()
    }

    private fun pickApp(isTop: Boolean) {
        val labels = apps.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(if (isTop) "Top app" else "Bottom app")
            .setItems(labels) { _, which ->
                if (isTop) topApp = apps[which] else bottomApp = apps[which]
                render()
            }
            .show()
    }

    private fun render() {
        findViewById<TextView>(R.id.topAppName).text = topApp?.label ?: "Tap to choose an app"
        findViewById<TextView>(R.id.bottomAppName).text = bottomApp?.label ?: "Tap to choose an app"
        val ready = topApp != null && bottomApp != null
        findViewById<MaterialButton>(R.id.launchSplitButton).isEnabled = ready
        findViewById<MaterialButton>(R.id.saveComboButton).isEnabled = ready
    }

    // ----- Saved combos -----

    private fun renderCombos() {
        val container = findViewById<LinearLayout>(R.id.combosContainer)
        container.removeAllViews()
        val combos = SplitComboStore.getAll(this)
        findViewById<TextView>(R.id.combosHeader).visibility = if (combos.isEmpty()) View.GONE else View.VISIBLE

        for (combo in combos) {
            val topLabel = AppCatalog.label(this, combo.topPackage)
            val bottomLabel = AppCatalog.label(this, combo.bottomPackage)

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_chip)
                setPadding(dp(16), dp(14), dp(12), dp(14))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) }
            }
            row.addView(TextView(this).apply {
                text = "$topLabel  +  $bottomLabel"
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(this).apply {
                text = "✕"
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setPadding(dp(12), dp(4), dp(6), dp(4))
                contentDescription = "Delete combo"
                setOnClickListener {
                    SplitComboStore.delete(this@SplitLauncherActivity, combo.id)
                    renderCombos()
                }
            })
            row.setOnClickListener { launchSplit(combo.topPackage, combo.bottomPackage) }
            container.addView(row)
        }
    }

    private fun saveCurrent() {
        val top = topApp ?: return
        val bottom = bottomApp ?: return
        if (top.packageName == bottom.packageName) {
            Toast.makeText(this, "Pick two different apps", Toast.LENGTH_SHORT).show()
            return
        }
        SplitComboStore.add(this, top.packageName, bottom.packageName)
        Toast.makeText(this, "Saved combo", Toast.LENGTH_SHORT).show()
        renderCombos()
    }

    // ----- Launching -----

    private fun launchCurrent() {
        val top = topApp ?: return
        val bottom = bottomApp ?: return
        if (top.packageName == bottom.packageName) {
            Toast.makeText(this, "Pick two different apps", Toast.LENGTH_SHORT).show()
            return
        }
        launchSplit(top.packageName, bottom.packageName)
    }

    private fun launchSplit(topPackage: String, bottomPackage: String) {
        // Preferred: the accessibility service uses the system split toggle.
        if (PermissionUtils.hasAccessibilityPermission(this) &&
            AppBlockAccessibilityService.launchSplit(topPackage, bottomPackage)
        ) {
            finish()
            return
        }
        // Fallback: launch-adjacent flags (works on many launchers).
        val top = packageManager.getLaunchIntentForPackage(topPackage)
        val bottom = packageManager.getLaunchIntentForPackage(bottomPackage)
        if (top == null || bottom == null) {
            Toast.makeText(this, "One of these apps can't be launched", Toast.LENGTH_SHORT).show()
            return
        }
        top.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(top)
        Handler(Looper.getMainLooper()).postDelayed({
            bottom.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
            )
            try {
                startActivity(bottom)
            } catch (e: Exception) {
                Toast.makeText(this, "Couldn't open split screen on this device", Toast.LENGTH_LONG).show()
            }
            finish()
        }, 400)
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
}
