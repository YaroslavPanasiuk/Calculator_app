package com.calculatorPackage.calculator

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.viewpager2.widget.ViewPager2
import com.calculatorPackage.calculator.SharedMethods.Companion.setTheme
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class MainActivity : AppCompatActivity() {

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        onConfigurationChanged(resources.configuration)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val tabLayoutMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.icon = resources.getDrawable(R.drawable.icon_calculate, theme)
                1 -> tab.icon = resources.getDrawable(R.drawable.icon_convert, theme)
            }
        }
        viewPager.adapter = MyAdapter(this)
        tabLayoutMediator.attach()
        findViewById<ImageButton>(R.id.options).setOnClickListener { openPopUpSettings(it as ImageButton) }
    }


    @SuppressLint("RestrictedApi")
    private fun openPopUpSettings(button: ImageButton) {
        val popup = PopupMenu(this, button)

        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.settings_menuitem -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    finish()
                }

                R.id.history_menuitem -> {
                    val intent = Intent(this, HistoryActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }
        if (popup.menu is MenuBuilder) (popup.menu as MenuBuilder).setOptionalIconsVisible(true)

        popup.inflate(R.menu.popup_menu_main)
        popup.show()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val topBar = findViewById<LinearLayout>(R.id.top_bar)
        when (newConfig.orientation) {
            ORIENTATION_LANDSCAPE -> {
                topBar.visibility = View.GONE
            }

            Configuration.ORIENTATION_PORTRAIT -> {
                topBar.visibility = View.VISIBLE
            }

            else -> {}
        }
    }
}