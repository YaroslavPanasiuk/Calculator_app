package com.calculatorPackage.calculator

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.PopupMenu
import com.calculatorPackage.calculator.SharedMethods.Companion.setTheme
import java.io.File
class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_activity)
        findViewById<ScrollView>(R.id.historyScrollView).post { findViewById<ScrollView>(R.id.historyScrollView).fullScroll(View.FOCUS_DOWN) }
        findViewById<ImageButton>(R.id.return_btn).setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        findViewById<ImageButton>(R.id.options).setOnClickListener { openPopUpSettings(it as ImageButton) }
        showHistory()
        showCurrentOperation()
    }
    private fun showHistory(){
        val file = File(applicationContext.filesDir, resources.getString(R.string.previous_operations_file))
        if(file.exists()){ findViewById<TextView>(R.id.previousResultsTextView).text =
            file.readLines().joinToString("\n\n") { it.split(";;").first() }
        }
    }

    private fun showCurrentOperation(){
        val prefs = getSharedPreferences(resources.getString(R.string.prefs_name), MODE_PRIVATE)
        val operationText = prefs.getString("operation", resources.getString(R.string.initial_operation))
        val resultText = prefs.getString("result", resources.getString(R.string.initial_operation))
        findViewById<TextView>(R.id.operation).text = operationText
        findViewById<TextView>(R.id.result).text = resultText
    }

    private fun openPopUpSettings(button: ImageButton){
        val popup = PopupMenu(this, button)
        popup.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.clear_history_menuitem -> {
                    File(applicationContext.filesDir, resources.getString(R.string.previous_operations_file)).writeText("")
                    getSharedPreferences(resources.getString(R.string.prefs_name), MODE_PRIVATE).edit().putString("previousOperations", "").apply()
                    showHistory()
                }
            }
            true
        }
        popup.inflate(R.menu.popup_menu_history)
        popup.show()
    }

}
