package com.calculatorPackage.calculator

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.text.Html
import android.util.TypedValue
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.gridlayout.widget.GridLayout
import com.calculatorPackage.calculator.SharedMethods.Companion.addNumberSeparator
import com.calculatorPackage.calculator.SharedMethods.Companion.getComma
import com.calculatorPackage.calculator.SharedMethods.Companion.getPeriod
import com.calculatorPackage.calculator.SharedMethods.Companion.setTheme
import com.calculatorPackage.calculator.SharedMethods.Companion.validateResult
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener


class ConverterActivity : ComponentActivity() {

    private lateinit var convertType: String
    private lateinit var data:JSONArray
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.converter_activity)
        onConfigurationChanged(resources.configuration)
        convertType = intent.getStringExtra(resources.getString(R.string.converterType)).toString()
        data = (JSONTokener(resources.openRawResource(R.raw.conversion_units).bufferedReader().use { it.readText() }).nextValue() as JSONObject).getJSONArray(convertType)
        setOnClicks()
        setSavedResults()
    }

    override fun onStop() {
        saveResults()
        super.onStop()
    }

    override fun onPause() {
        saveResults()
        super.onPause()
    }

    private fun saveResults(){
        val editor = getSharedPreferences("conversions", Context.MODE_PRIVATE).edit()
        editor.putInt("${convertType}units1", findViewById<Spinner>(R.id.units1).selectedItemPosition)
        editor.putInt("${convertType}units2", findViewById<Spinner>(R.id.units2).selectedItemPosition)
        editor.putString("${convertType}value1", findViewById<TextView>(R.id.value1).text.toString())
        editor.putString("${convertType}value2", findViewById<TextView>(R.id.value2).text.toString())
        editor.putBoolean("${convertType}value1IsSelected", isSelected(findViewById(R.id.value1)))
        val settings = getSharedPreferences(resources.getString(R.string.prefs_name), Context.MODE_PRIVATE).edit()
        settings.putString("prevComma", getComma(this)).toString()
        settings.putString("prevPeriod", getComma(this)).toString()
        settings.apply()
        if(convertType == resources.getString(R.string.temp_btn)){
            editor.putBoolean("${convertType}valueIsPositive", findViewById<ImageButton>(R.id.toggle_negative_btn).tag != "minus")
        }
        editor.apply()
    }

    private fun setSavedResults(){
        val prefs = getSharedPreferences("conversions", Context.MODE_PRIVATE)
        findViewById<Spinner>(R.id.units1).setSelection(prefs.getInt("${convertType}units1", 1))
        findViewById<Spinner>(R.id.units2).setSelection(prefs.getInt("${convertType}units2", 1))
        val prevComma = getSharedPreferences(resources.getString(R.string.prefs_name), Context.MODE_PRIVATE).getString("prevComma", getComma(this)).toString()
        val prevPeriod = getSharedPreferences(resources.getString(R.string.prefs_name), Context.MODE_PRIVATE).getString("prevPeriod", getPeriod(this)).toString()
        var value1text = prefs.getString("${convertType}value1", resources.getString(R.string.initial_operation)).toString()
        var value2text = prefs.getString("${convertType}value2", resources.getString(R.string.initial_operation)).toString()
        if(prevComma != getComma(this)){
            value1text = value1text.replace(prevComma, "@").replace(prevPeriod, getPeriod(this)).replace("@", getComma(this))
            value2text = value2text.replace(prevComma, "@").replace(prevPeriod, getPeriod(this)).replace("@", getComma(this))
        }
        findViewById<TextView>(R.id.value1).text = value1text
        findViewById<TextView>(R.id.value2).text = value2text
        if(convertType == resources.getString(R.string.temp_btn)){
            if(!prefs.getBoolean("${convertType}valueIsPositive", true)) {toggleMinus()}
        }
        if(prefs.getBoolean("${convertType}value1IsSelected", true)) selectValue(findViewById(R.id.value1)) else selectValue(findViewById(R.id.value2))
    }

    @SuppressLint("ServiceCast")
    private fun setOnClicks(){
        val units1 = findViewById<Spinner>(R.id.units1)
        val units2 = findViewById<Spinner>(R.id.units2)
        val value1 = findViewById<TextView>(R.id.value1)
        val value2 = findViewById<TextView>(R.id.value2)
        val unit1 = findViewById<TextView>(R.id.unit1)
        val unit2 = findViewById<TextView>(R.id.unit2)
        val legend = findViewById<TextView>(R.id.legend)
        val buttonDelete = findViewById<ImageButton>(R.id.buttonDelete)
        val buttonMinus = findViewById<ImageButton>(R.id.toggle_negative_btn)
        findViewById<ImageButton>(R.id.return_btn).setOnClickListener{onBackPressedDispatcher.onBackPressed()}
        buttonDelete.setOnClickListener { deleteAction() }
        buttonDelete.setOnLongClickListener {
            selectedValue().text = resources.getString(R.string.initial_operation)
            showConversion()
            true
        }
        val buttonPeriod = findViewById<Button>(R.id.buttonPeriod)
        buttonPeriod.text = getPeriod(this)
        buttonPeriod.setOnClickListener { enterPeriod() }
        listOf<Button>(
            findViewById(R.id.buttonOne),
            findViewById(R.id.buttonTwo),
            findViewById(R.id.buttonThree),
            findViewById(R.id.buttonFour),
            findViewById(R.id.buttonFive),
            findViewById(R.id.buttonSix),
            findViewById(R.id.buttonSeven),
            findViewById(R.id.buttonEight),
            findViewById(R.id.buttonNine),
            findViewById(R.id.buttonZero)
        ).forEach { btn -> btn.setOnClickListener { enterNumber((it as Button).text.toString()) } }

        value1.setOnClickListener { selectValue(value1) }
        value2.setOnClickListener { selectValue(value2) }
        value1.setOnLongClickListener { this.copyToClipboard(value1.text) }
        value2.setOnLongClickListener { this.copyToClipboard(value2.text) }

        legend.text = convertType

        if(convertType == resources.getString(R.string.temp_btn)){
            buttonDelete.layoutParams = GridLayout.LayoutParams(
                GridLayout.spec(0, GridLayout.FILL),
                GridLayout.spec(3, GridLayout.FILL)
            )
            buttonMinus.visibility = View.VISIBLE
        }
        buttonMinus.setOnClickListener {
            toggleMinus()
            enterSign()
        }


        val adapter = ArrayAdapter(this, R.layout.drop_down_list_item,  data.getValues("name"))

        units1.adapter = adapter
        units2.adapter = adapter
        units1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(view !is TextView){return}
                view.text = Html.fromHtml(data.getJSONObject(position).getString("shortened"), Html.FROM_HTML_MODE_COMPACT)
                unit1.text = data.getJSONObject(position).getString("name")
                showConversion()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        units2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(view !is TextView){return}
                view.text = Html.fromHtml(data.getJSONObject(position).getString("shortened"), Html.FROM_HTML_MODE_COMPACT)
                unit2.text = data.getJSONObject(position).getString("name")
                showConversion()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun Context.copyToClipboard(text: CharSequence): Boolean{
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = ClipData.newPlainText("Text copied!",text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
        return true
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun toggleMinus() {
        val buttonMinus = findViewById<ImageButton>(R.id.toggle_negative_btn)
        buttonMinus.tag = if(buttonMinus.tag == "minus") "plus" else "minus"
        when(buttonMinus.tag){
            "plus" -> buttonMinus.setImageDrawable(resources.getDrawable(R.drawable.plus_active_sign, theme))
            "minus" -> buttonMinus.setImageDrawable(resources.getDrawable(R.drawable.minus_active_sign, theme))
        }

    }

    private fun enterSign() {
        selectedValue().apply {
            text = when(text.first().toString()){
                resources.getString(R.string.minus_btn) -> text.drop(1)
                else -> "${resources.getString(R.string.minus_btn)}$text"
            }
        }
        showConversion()
    }

    private fun JSONArray.getValues(key: String): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until this.length()) {
            list.add(this.getJSONObject(i).getString(key))
        }
        return list
    }

    private fun JSONArray.getObjectWithName(name: String): JSONObject {
        for (i in 0 until this.length()) {
            if(this.getJSONObject(i).getString("name") == name){
                return this.getJSONObject(i)
            }
        }
        throw Exception("Not found json object with name $name")
    }

    @ColorInt
    fun getColorFromAttr(
        @AttrRes attrColor: Int,
        typedValue: TypedValue = TypedValue(),
        resolveRefs: Boolean = true
    ): Int {
        theme.resolveAttribute(attrColor, typedValue, resolveRefs)
        return typedValue.data
    }

    @SuppressLint("SetTextI18n")
    private fun enterNumber(number:String){
        if("-?0*".toRegex().matches(selectedValue().text) || selectedValue().tag == resources.getString(R.string.recently_selected)){
            selectedValue().text = if(findViewById<ImageButton>(R.id.toggle_negative_btn).tag == "minus") resources.getString(R.string.minus_btn) else ""
        }
        if(selectedValue().tag == resources.getString(R.string.recently_selected)){selectedValue().tag = null}
        selectedValue().apply { text = "$text$number" }
        showConversion()
    }
    @SuppressLint("SetTextI18n")
    private fun enterPeriod(){
        if(selectedValue().text.isEmpty()){ selectedValue().text = resources.getString(R.string.initial_operation) }
        if(selectedValue().text.contains(getPeriod(this))){ return }
        selectedValue().apply{text = "$text${getPeriod(context)}"}
    }

    private fun deleteAction(count: Int = 1){
        when{
            "[Ee]-?\\d*".toRegex().containsMatchIn(selectedValue().text) -> selectedValue().apply { text = text.replace("[Ee]-?\\d*".toRegex(), "") }
            else -> selectedValue().apply { text = text.dropLast(count) }
        }
        if(selectedValue().text.isEmpty()){selectedValue().text = resources.getString(R.string.initial_operation)}
        showConversion()
    }
    private fun selectValue(value: TextView){
        value.tag = resources.getString(R.string.recently_selected)
        value.setTextColor(getColorFromAttr(androidx.appcompat.R.attr.colorAccent))
        if((value.text.first().toString() == resources.getString(R.string.minus_btn)) != (findViewById<View?>(R.id.toggle_negative_btn).tag == "minus")){toggleMinus()}
        (if(value == findViewById<TextView>(R.id.value1)) findViewById(R.id.value2) else findViewById<TextView>(R.id.value1)).setTextColor(getColorFromAttr(R.attr.textColorSecondary))
    }

    private fun isSelected(unit: TextView): Boolean{
        return unit.currentTextColor == getColorFromAttr(androidx.appcompat.R.attr.colorAccent)
    }

    private fun selectedValue():TextView = if(isSelected(findViewById(R.id.value1))) findViewById(R.id.value1) else findViewById(R.id.value2)
    private fun unSelectedValue():TextView = if(isSelected(findViewById(R.id.value1))) findViewById(R.id.value2) else findViewById(R.id.value1)
    private fun selectedUnit():String = if(isSelected(findViewById(R.id.value1))) findViewById<Spinner>(R.id.units1).selectedItem as String else findViewById<Spinner>(R.id.units2).selectedItem as String
    private fun unSelectedUnit():String = if(isSelected(findViewById(R.id.value2))) findViewById<Spinner>(R.id.units1).selectedItem as String else findViewById<Spinner>(R.id.units2).selectedItem as String
    private fun convertLinear(value:Double, unitsFrom: JSONObject, unitsTo:JSONObject):Double{
        return value * unitsFrom.getDouble("coefficient") / unitsTo.getDouble("coefficient")
    }

    private fun toKelvin(value: Double, units: String): Double{
        return when(units){
            "Celsius" -> value + 273.15
            "Fahrenheit" -> (value-32)/1.8 + 273.15
            "Rankine" -> value/1.8
            else -> value
        }
    }

    private fun fromKelvin(value: Double, units: String): Double{
        return when(units){
            "Celsius" -> value - 273.15
            "Fahrenheit" -> (value-273.5)*1.8 + 32
            "Rankine" -> value*1.8
            else -> value
        }
    }

    private fun convertTemperature(value:Double, unitsFrom: JSONObject, unitsTo:JSONObject):Double{
        return fromKelvin(toKelvin(value, unitsFrom.getString("name")), unitsTo.getString("name"))
    }

    private fun convert(): Double {
        val unitsFrom = data.getObjectWithName(selectedUnit())
        val unitsTo = data.getObjectWithName(unSelectedUnit())
        return try {
            val value = selectedValue().text.toString().double()
            when (convertType) {
                resources.getString(R.string.temp_btn) -> convertTemperature(value, unitsFrom, unitsTo)
                else -> convertLinear(value, unitsFrom, unitsTo)
            }
        } catch (_:NumberFormatException){
            Double.NaN
        }
    }

    private fun String.double():Double{
        if(this == resources.getString(R.string.minus_btn) || this == ""){return 0.0}
        return this.replace(getComma(applicationContext), "").replace(getPeriod(applicationContext), ".").toDouble()
    }

    private fun roundHighOrders(text: String): String{
        var result = text
        if("[Ee]-?\\d*".toRegex().containsMatchIn(text)){
            while (result.length > resources.getInteger(R.integer.converter_max_length)){
                result = result.replace("\\d[Ee]-?\\d*".toRegex()){it.value.drop(1)}
            }
        }
        return result
    }

    private fun showConversion(){
        unSelectedValue().text = roundHighOrders(validateResult(convert().toString(), this, 10000))
        selectedValue().apply { text = addNumberSeparator(text.toString(), context) }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val screen = findViewById<LinearLayout>(R.id.screen)
        val display = findViewById<LinearLayout>(R.id.display)
        val grid = findViewById<GridLayout>(R.id.grid)
        when(newConfig.orientation){
            Configuration.ORIENTATION_LANDSCAPE -> {
                screen.orientation = LinearLayout.HORIZONTAL
                display.layoutParams = LinearLayout.LayoutParams(0, android.widget.GridLayout.LayoutParams.MATCH_PARENT, 1f)
                grid.layoutParams = LinearLayout.LayoutParams(0, android.widget.GridLayout.LayoutParams.MATCH_PARENT, 1f)
                screen.removeAllViews()
                screen.addView(grid)
                screen.addView(display)
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                screen.orientation = LinearLayout.VERTICAL
                display.layoutParams = LinearLayout.LayoutParams(android.widget.GridLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                grid.layoutParams = LinearLayout.LayoutParams(android.widget.GridLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                screen.removeAllViews()
                screen.addView(display)
                screen.addView(grid)
            }
            else -> {}
        }
    }

}


