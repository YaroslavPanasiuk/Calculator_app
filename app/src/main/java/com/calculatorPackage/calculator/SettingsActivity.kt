package com.calculatorPackage.calculator

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageButton
import android.widget.RadioGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.calculatorPackage.calculator.SharedMethods.Companion.setTheme

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        val settingsFragment = SettingsFragment()
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, settingsFragment)
                .commit()
        }
        val radioGroup = findViewById<RadioGroup>(R.id.theme_radio_group)
        val themeId = getSharedPreferences(resources.getString(R.string.settings_menuitem), MODE_PRIVATE).getInt(resources.getString(R.string.theme_setting), -1)

        findViewById<ImageButton>(R.id.return_btn).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        radioGroup.check(when(themeId){
            R.style.Theme_CalculatorOrange -> R.id.theme_orange
            R.style.Theme_CalculatorPurple -> R.id.theme_purple
            R.style.Theme_CalculatorRed -> R.id.theme_red
            R.style.Theme_CalculatorGreen -> R.id.theme_green
            R.style.Theme_CalculatorVanilla -> R.id.theme_vanilla
            else -> R.id.theme_light_dark
        })
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            writeTheme(when(checkedId) {
                R.id.theme_orange -> { R.style.Theme_CalculatorOrange }
                R.id.theme_purple -> { R.style.Theme_CalculatorPurple }
                R.id.theme_red -> { R.style.Theme_CalculatorRed }
                R.id.theme_green -> { R.style.Theme_CalculatorGreen }
                R.id.theme_vanilla -> { R.style.Theme_CalculatorVanilla }
                else -> { -1 }
            })
            finish()
            startActivity(intent)
        }
        onBackPressedDispatcher.addCallback(object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(applicationContext, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        })

    }

    private fun writeTheme(id: Int){
        val editor = getSharedPreferences(resources.getString(R.string.settings_menuitem), MODE_PRIVATE).edit()
        editor.putInt(resources.getString(R.string.theme_setting), id)
        editor.apply()
    }

    class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener  {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            preferenceManager.sharedPreferencesName = resources.getString(R.string.settings_menuitem)
            addPreferencesFromResource(R.xml.user_preferences)
        }
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            //setPreferencesFromResource(R.xml.user_preferences, rootKey)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when(key){
                resources.getString(R.string.separator_setting) -> {
                    val separationType = preferenceScreen.findPreference<ListPreference>(resources.getString(R.string.separator_type_setting))!!
                    if(sharedPreferences?.getString(key, "") == resources.getString(R.string.separator_none)){
                        separationType.isEnabled = false
                        separationType.layoutResource = R.layout.list_preference_hidden
                    }
                    else{
                        separationType.isEnabled = true
                        separationType.layoutResource = R.layout.preference_regular
                    }
                    if(sharedPreferences?.getString(key, "") == sharedPreferences?.getString(resources.getString(R.string.period_setting), "q")){
                        preferenceScreen.findPreference<ListPreference>(resources.getString(R.string.period_setting))!!.apply {
                            value = if(value == resources.getString(R.string.comma_sign)) resources.getString(R.string.period_sign) else resources.getString(R.string.comma_sign)
                        }
                    }
                }
                resources.getString(R.string.period_setting) -> {
                    if(sharedPreferences?.getString(key, "") == sharedPreferences?.getString(resources.getString(R.string.separator_setting), "q")){
                        preferenceScreen.findPreference<ListPreference>(resources.getString(R.string.separator_setting))!!.apply {
                            value = if(value == resources.getString(R.string.comma_sign)) resources.getString(R.string.period_sign) else resources.getString(R.string.comma_sign)
                        }
                    }
                }
                else -> {}
            }
        }
        override fun onResume() {
            super.onResume()
            preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        }
    }
}