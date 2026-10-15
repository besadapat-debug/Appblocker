package com.example.appblocker.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.appblocker.AppCatalog
import com.example.appblocker.DayStat
import com.example.appblocker.R
import com.example.appblocker.ScheduleFormat
import com.example.appblocker.UsageTracker
import com.google.android.material.button.MaterialButton

// Screen time, opens, blocked attempts and silenced notifications, from
// AppBlocker's own tracker (no Usage Access permission needed).
class InsightsFragment : Fragment() {

    private var showWeek = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_insights, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<MaterialButton>(R.id.todayButton).setOnClickListener { showWeek = false; render(view) }
        view.findViewById<MaterialButton>(R.id.weekButton).setOnClickListener { showWeek = true; render(view) }
    }

    override fun onResume() {
        super.onResume()
        view?.let { render(it) }
    }

    private fun render(view: View) {
        val context = requireContext()
        styleToggle(view.findViewById(R.id.todayButton), !showWeek)
        styleToggle(view.findViewById(R.id.weekButton), showWeek)

        val stats: Map<String, DayStat> =
            if (showWeek) UsageTracker.lastDays(context, 7) else UsageTracker.today(context)

        val totalTime = stats.values.sumOf { it.timeMs }
        val totalOpens = stats.values.sumOf { it.launches }
        val totalBlocked = stats.values.sumOf { it.blocked }
        val totalNotifications = stats.values.sumOf { it.notifications }

        bindStat(view.findViewById(R.id.statTime), ScheduleFormat.durationMs(totalTime), "Screen time in apps")
        bindStat(view.findViewById(R.id.statOpens), "$totalOpens", "App opens")
        bindStat(view.findViewById(R.id.statBlocked), "$totalBlocked", "Blocked attempts")
        bindStat(view.findViewById(R.id.statNotifications), "$totalNotifications", "Notifications silenced")

        val list = view.findViewById<LinearLayout>(R.id.appUsageList)
        list.removeAllViews()
        val sorted = stats.entries
            .filter { it.value.timeMs >= 60_000L || it.value.blocked > 0 }
            .sortedByDescending { it.value.timeMs }
            .take(25)
        view.findViewById<TextView>(R.id.emptyStateText).visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE

        val maxMillis = sorted.maxOfOrNull { it.value.timeMs }?.coerceAtLeast(1L) ?: 1L
        val inflater = LayoutInflater.from(context)
        for ((pkg, s) in sorted) {
            val row = inflater.inflate(R.layout.item_app_usage, list, false)
            row.findViewById<TextView>(R.id.appName).text = AppCatalog.label(context, pkg)
            val details = mutableListOf(ScheduleFormat.durationMs(s.timeMs), "${s.launches} opens")
            if (s.blocked > 0) details.add("${s.blocked} blocked")
            row.findViewById<TextView>(R.id.appDuration).text = details.joinToString(" · ")

            val bar = row.findViewById<View>(R.id.usageBar)
            row.post {
                val trackWidth = (bar.parent as View).width
                bar.layoutParams = bar.layoutParams.apply {
                    width = (trackWidth * (s.timeMs.toFloat() / maxMillis)).toInt()
                }
            }
            list.addView(row)
        }
    }

    private fun bindStat(card: View, value: String, label: String) {
        card.findViewById<TextView>(R.id.statValue).text = value
        card.findViewById<TextView>(R.id.statLabel).text = label
    }

    private fun styleToggle(button: MaterialButton, selected: Boolean) {
        val context = requireContext()
        button.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, if (selected) R.color.accent_blue else R.color.card_dark)
        )
        button.setTextColor(ContextCompat.getColor(context, if (selected) R.color.text_primary else R.color.text_secondary))
    }
}
