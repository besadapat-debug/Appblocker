package com.example.appblocker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.appblocker.fragments.BlockingFragment
import com.example.appblocker.fragments.InsightsFragment
import com.example.appblocker.fragments.ProfileFragment
import com.example.appblocker.fragments.StrictModeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

// The app now follows a 4-tab structure (Blocking / Strict Mode / Insights /
// Profile), same shape as AppBlock's navigation. Each tab is its own
// Fragment; this Activity just hosts the bottom nav and swaps them in.
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_blocking
            showFragment(BlockingFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_blocking -> BlockingFragment()
                R.id.nav_strict_mode -> StrictModeFragment()
                R.id.nav_insights -> InsightsFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> BlockingFragment()
            }
            showFragment(fragment)
            true
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
