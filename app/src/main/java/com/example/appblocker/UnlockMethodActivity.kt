package com.example.appblocker

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class UnlockMethodActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SCOPE = "scope"
    }

    private var scope: String = ""

    private val descriptions = mapOf(
        UnlockMethod.TIMER to "Strict Mode can be turned off once this much time has passed since you turned it on.",
        UnlockMethod.FOLLOW_SCHEDULES to "Strict Mode can be turned off only outside a weekly schedule you set.",
        UnlockMethod.CHARGER to "Strict Mode will be deactivated by putting your device on a charger.",
        UnlockMethod.COOLDOWN to "Press Unlock, then wait out a countdown before Strict Mode turns off.",
        UnlockMethod.PIN_CODE to "You'll be able to deactivate Strict Mode using the set PIN code."
    )

    private var selected = mutableSetOf<UnlockMethod>()
    private val descriptionViews = mutableMapOf<UnlockMethod, TextView>()

    private val TIMER_CHOICES = listOf(15, 30, 60, 120, 240, 480, 720, 1440)
    private val COOLDOWN_CHOICES = listOf(10, 30, 60, 120, 300, 600)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unlock_method)

        scope = intent.getStringExtra(EXTRA_SCOPE) ?: ""

        // Switching to an easier unlock method while this lock is active would be a loophole.
        if (StrictModeStore.isActive(this, scope)) {
            Toast.makeText(this, "Unlock methods are locked while this lock is active", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        selected = UnlockMethodStore.getSelectedMethods(this, scope).toMutableSet()

        findViewById<ImageButton>(R.id.closeButton).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.saveMethodsButton).setOnClickListener {
            if (UnlockMethod.PIN_CODE in selected && !UnlockMethodStore.hasPinSet(this, scope)) {
                Toast.makeText(this, "Set a PIN first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            UnlockMethodStore.setSelectedMethods(this, selected, scope)
            finish()
        }

        buildMethodRows()
    }

    private fun buildMethodRows() {
        val container = findViewById<LinearLayout>(R.id.methodsList)
        val inflater = LayoutInflater.from(this)

        for (method in UnlockMethod.values()) {
            val itemView = inflater.inflate(R.layout.item_unlock_method, container, false)
            itemView.findViewById<TextView>(R.id.methodTitle).text = method.label
            val descView = itemView.findViewById<TextView>(R.id.methodDescription)
            descriptionViews[method] = descView
            refreshDescription(method)

            val difficultyView = itemView.findViewById<TextView>(R.id.methodDifficulty)
            difficultyView.text = method.difficulty
            difficultyView.setTextColor(difficultyColor(method.difficulty))

            val checkIcon = itemView.findViewById<ImageView>(R.id.checkCircle)
            checkIcon.setImageResource(
                if (method in selected) R.drawable.ic_circle_checked else R.drawable.ic_circle_unchecked
            )

            itemView.findViewById<FrameLayout>(R.id.checkCircleFrame).setOnClickListener {
                toggleMethod(method, checkIcon)
            }
            itemView.findViewById<LinearLayout>(R.id.methodRow).setOnClickListener {
                when {
                    method in selected && method == UnlockMethod.TIMER -> promptTimerMinutes()
                    method in selected && method == UnlockMethod.COOLDOWN -> promptCooldownSeconds()
                    method in selected && method == UnlockMethod.FOLLOW_SCHEDULES -> openSchedule()
                    else -> toggleMethod(method, checkIcon)
                }
            }

            container.addView(itemView)
        }
    }

    private fun toggleMethod(method: UnlockMethod, checkIcon: ImageView) {
        if (method in selected) {
            selected.remove(method)
            checkIcon.setImageResource(R.drawable.ic_circle_unchecked)
            refreshDescription(method)
            return
        }

        selected.add(method)
        checkIcon.setImageResource(R.drawable.ic_circle_checked)
        refreshDescription(method)

        when (method) {
            UnlockMethod.PIN_CODE -> promptSetPin()
            UnlockMethod.TIMER -> promptTimerMinutes()
            UnlockMethod.COOLDOWN -> promptCooldownSeconds()
            UnlockMethod.FOLLOW_SCHEDULES -> openSchedule()
            else -> Unit
        }
    }

    private fun promptSetPin() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_set_pin, null)
        val input = view.findViewById<EditText>(R.id.pinInput)

        AlertDialog.Builder(this)
            .setTitle("Set PIN")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val pin = input.text.toString()
                if (pin.length >= 4) {
                    UnlockMethodStore.setPin(this, pin, scope)
                    refreshDescription(UnlockMethod.PIN_CODE)
                } else {
                    Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                    selected.remove(UnlockMethod.PIN_CODE)
                }
            }
            .setNegativeButton("Cancel") { _, _ -> selected.remove(UnlockMethod.PIN_CODE) }
            .setOnCancelListener { selected.remove(UnlockMethod.PIN_CODE) }
            .show()
    }

    private fun openSchedule() {
        startActivity(android.content.Intent(this, StrictScheduleActivity::class.java)
            .putExtra(StrictScheduleActivity.EXTRA_SCOPE, scope))
    }

    private fun refreshDescription(method: UnlockMethod) {
        val base = descriptions[method] ?: ""
        val setting = when (method) {
            UnlockMethod.TIMER -> "\nSet to: ${formatMinutes(UnlockMethodStore.getTimerMinutes(this, scope))}  (tap to change)"
            UnlockMethod.COOLDOWN -> "\nSet to: ${formatSeconds(UnlockMethodStore.getCooldownSeconds(this, scope))}  (tap to change)"
            UnlockMethod.PIN_CODE -> if (UnlockMethodStore.hasPinSet(this, scope)) "\nPIN is set" else ""
            UnlockMethod.FOLLOW_SCHEDULES ->
                if (StrictScheduleStore.hasSchedule(this, scope)) "\nTap to edit your locked days and times"
                else "\nTap to set the days and times to stay locked"
            else -> ""
        }
        descriptionViews[method]?.text = base + if (method in selected) setting else ""
    }

    private fun promptTimerMinutes() {
        val labels = TIMER_CHOICES.map { formatMinutes(it) } + "Custom (minutes)…"
        AlertDialog.Builder(this)
            .setTitle("Timer length")
            .setItems(labels.toTypedArray()) { _, which ->
                if (which < TIMER_CHOICES.size) {
                    UnlockMethodStore.setTimerMinutes(this, TIMER_CHOICES[which], scope)
                    refreshDescription(UnlockMethod.TIMER)
                } else {
                    promptNumber("Timer length", "minutes", UnlockMethodStore.getTimerMinutes(this, scope), 1, 7 * 24 * 60) {
                        UnlockMethodStore.setTimerMinutes(this, it, scope)
                        refreshDescription(UnlockMethod.TIMER)
                    }
                }
            }
            .show()
    }

    private fun promptCooldownSeconds() {
        val labels = COOLDOWN_CHOICES.map { formatSeconds(it) } + "Custom (seconds)…"
        AlertDialog.Builder(this)
            .setTitle("Cooldown length")
            .setItems(labels.toTypedArray()) { _, which ->
                if (which < COOLDOWN_CHOICES.size) {
                    UnlockMethodStore.setCooldownSeconds(this, COOLDOWN_CHOICES[which], scope)
                    refreshDescription(UnlockMethod.COOLDOWN)
                } else {
                    promptNumber("Cooldown length", "seconds", UnlockMethodStore.getCooldownSeconds(this, scope), 5, 24 * 60 * 60) {
                        UnlockMethodStore.setCooldownSeconds(this, it, scope)
                        refreshDescription(UnlockMethod.COOLDOWN)
                    }
                }
            }
            .show()
    }

    // Plain number box with the unit written next to it.
    private fun promptNumber(title: String, unit: String, current: Int, min: Int, max: Int, onSaved: (Int) -> Unit) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_number_input, null)
        val input = view.findViewById<EditText>(R.id.numberInput)
        view.findViewById<TextView>(R.id.numberUnit).text = unit
        view.findViewById<TextView>(R.id.numberHint).text = "Enter a number of $unit ($min–$max)."
        input.setText(current.toString())
        input.setSelection(input.text.length)

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val value = input.text.toString().toIntOrNull()
                if (value != null && value in min..max) {
                    onSaved(value)
                } else {
                    Toast.makeText(this, "Enter a number of $unit between $min and $max", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatMinutes(minutes: Int): String {
        val d = minutes / 1440
        val h = (minutes % 1440) / 60
        val m = minutes % 60
        val parts = mutableListOf<String>()
        if (d > 0) parts.add(if (d == 1) "1 day" else "$d days")
        if (h > 0) parts.add(if (h == 1) "1 hour" else "$h hours")
        if (m > 0) parts.add(if (m == 1) "1 minute" else "$m minutes")
        return parts.joinToString(" ").ifEmpty { "0 minutes" }
    }

    private fun formatSeconds(seconds: Int): String = when {
        seconds < 60 -> "$seconds seconds"
        seconds % 60 == 0 -> formatMinutes(seconds / 60)
        else -> "${seconds / 60} min ${seconds % 60} sec"
    }

    private fun difficultyColor(difficulty: String): Int = when (difficulty) {
        "EASY" -> Color.parseColor("#4CAF50")
        "MEDIUM" -> Color.parseColor("#3E7BFA")
        else -> Color.parseColor("#B388FF")
    }
}
