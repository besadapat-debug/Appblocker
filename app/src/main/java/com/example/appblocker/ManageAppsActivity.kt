package com.example.appblocker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton

// App picker for one block list. Returns the chosen packages, plus any
// websites the user agreed to add (pick Instagram -> "also block instagram.com?").
class ManageAppsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SELECTED = "selected_packages"
        const val EXTRA_WEBSITES = "current_websites"
        const val EXTRA_ADDED_WEBSITES = "added_websites"
        const val EXTRA_LIST_NAME = "list_name"
        const val EXTRA_ALLOW_MODE = "allow_mode"
        const val EXTRA_ADD_ONLY = "add_only"
    }

    private var allApps: List<AppInfo> = emptyList()
    private var shownApps: List<AppInfo> = emptyList()
    private val selected = LinkedHashSet<String>()
    private val websites = mutableSetOf<String>()
    private val addedWebsites = LinkedHashSet<String>()
    private lateinit var adapter: AppAdapter
    private var allowMode = false
    private var addOnly = false
    private val original = LinkedHashSet<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_manage_apps)
        findViewById<ImageButton>(R.id.closeButton).setOnClickListener { finish() }

        selected.addAll(intent.getStringArrayListExtra(EXTRA_SELECTED) ?: arrayListOf())
        websites.addAll(intent.getStringArrayListExtra(EXTRA_WEBSITES) ?: arrayListOf())

        allowMode = intent.getBooleanExtra(EXTRA_ALLOW_MODE, false)
        addOnly = intent.getBooleanExtra(EXTRA_ADD_ONLY, false)
        original.addAll(intent.getStringArrayListExtra(EXTRA_SELECTED) ?: arrayListOf())
        val listName = intent.getStringExtra(EXTRA_LIST_NAME).orEmpty().trim()
        val verb = if (allowMode) "allow" else "block"
        findViewById<TextView>(R.id.pickerSubtitle).text =
            if (listName.isNotEmpty()) "Apps to $verb in \"$listName\"" else "Choose the apps to $verb in this list."

        // Already-chosen apps first, then alphabetical.
        allApps = AppCatalog.launcherApps(this)
            .sortedWith(compareBy<AppInfo>({ it.packageName !in selected }, { it.label.lowercase() }))
        shownApps = allApps

        adapter = AppAdapter()
        val listView = findViewById<ListView>(R.id.appListView)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ -> toggle(shownApps[position]) }

        findViewById<EditText>(R.id.searchInput).doAfterTextChanged { text ->
            val q = text?.toString()?.trim()?.lowercase().orEmpty()
            shownApps = if (q.isEmpty()) allApps else allApps.filter { it.label.lowercase().contains(q) }
            adapter.notifyDataSetChanged()
        }

        findViewById<MaterialButton>(R.id.saveBlockedAppsButton).setOnClickListener { finishWithResult() }
        findViewById<MaterialButton>(R.id.selectAllButton).setOnClickListener { toggleSelectAll() }
        updateSaveLabel()
        updateSelectAllLabel()
    }

    // Select all apps, or clear the selection if everything's already selected.
    // In allow mode this makes "allow everything, then deselect the few to block"
    // a one-tap start.
    private fun toggleSelectAll() {
        val allSelected = allApps.all { it.packageName in selected }
        if (allSelected) {
            // Clear - but keep add-only originals that can't be removed.
            if (addOnly) selected.retainAll(original) else selected.clear()
        } else {
            selected.addAll(allApps.map { it.packageName })
        }
        adapter.notifyDataSetChanged()
        updateSaveLabel()
        updateSelectAllLabel()
    }

    private fun updateSelectAllLabel() {
        val allSelected = allApps.isNotEmpty() && allApps.all { it.packageName in selected }
        findViewById<MaterialButton>(R.id.selectAllButton).text = if (allSelected) "Clear" else "Select all"
    }

    private fun toggle(app: AppInfo) {
        // Add-only (block mode): can't untick an app that was already on the list.
        if (addOnly && app.packageName in original && app.packageName in selected) {
            Toast.makeText(this, "Strict Mode: you can add apps, but not remove them", Toast.LENGTH_SHORT).show()
            return
        }
        if (!selected.remove(app.packageName)) {
            selected.add(app.packageName)
            if (!allowMode) suggestWebsitesForApp(app)
        }
        adapter.notifyDataSetChanged()
        updateSaveLabel()
        updateSelectAllLabel()
    }

    // Blocking the Instagram app? Offer to block instagram.com too.
    private fun suggestWebsitesForApp(app: AppInfo) {
        val domains = LinkedTargets.domainsForApp(app.packageName).filter { it !in websites }
        if (domains.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle("Also block ${domains.joinToString(" & ")}?")
            .setMessage("Otherwise ${app.label} can still be used in a web browser.")
            .setPositiveButton("Block website too") { _, _ ->
                websites.addAll(domains)
                addedWebsites.addAll(domains)
                Toast.makeText(this, "Added ${domains.joinToString(", ")}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Just the app", null)
            .show()
    }

    private fun updateSaveLabel() {
        val count = selected.count { pkg -> allApps.any { it.packageName == pkg } }
        findViewById<MaterialButton>(R.id.saveBlockedAppsButton).text =
            if (count == 0) "Done" else "Done ($count selected)"
    }

    private fun finishWithResult() {
        // "Block reinstalled applications" (Strict Mode option): keep apps
        // that were on the list but are uninstalled, so a reinstall is still
        // blocked. Otherwise drop them.
        if (!StrictOptionsStore.isBlockReinstalledApps(this)) {
            val installed = allApps.map { it.packageName }.toSet()
            selected.retainAll(installed)
        }
        val data = Intent().apply {
            putStringArrayListExtra(EXTRA_SELECTED, ArrayList(selected))
            putStringArrayListExtra(EXTRA_ADDED_WEBSITES, ArrayList(addedWebsites))
        }
        setResult(RESULT_OK, data)
        finish()
    }

    private inner class AppAdapter : BaseAdapter() {
        override fun getCount() = shownApps.size
        override fun getItem(position: Int) = shownApps[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = (convertView ?: LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_multiple_choice, parent, false)) as CheckedTextView
            val app = shownApps[position]
            view.text = app.label
            view.isChecked = app.packageName in selected
            view.setTextColor(ContextCompat.getColor(parent.context, R.color.text_primary))
            return view
        }
    }
}
