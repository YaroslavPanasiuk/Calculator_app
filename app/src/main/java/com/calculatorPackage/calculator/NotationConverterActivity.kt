package com.calculatorPackage.calculator

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.widget.doAfterTextChanged
import com.calculatorPackage.calculator.SharedMethods.Companion.getColorFromAttr
import com.calculatorPackage.calculator.SharedMethods.Companion.getPeriod
import com.calculatorPackage.calculator.SharedMethods.Companion.setTheme
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.abs
import kotlin.math.pow


class NotationConverterActivity : ComponentActivity() {

    private lateinit var data:JSONArray
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.number_system_converter_activity)
        data = (JSONTokener(resources.openRawResource(R.raw.conversion_units).bufferedReader().use { it.readText() }).nextValue() as JSONObject).getJSONArray(resources.getString(R.string.notation_btn))
        setOnClicks()
        setSavedResults()
    }

    override fun onStop() {
        saveResults()
        super.onStop()
    }

    private fun saveResults(){
        val editor = getSharedPreferences("conversions", Context.MODE_PRIVATE).edit()
        editor.putInt("${resources.getString(R.string.notation_btn)}units1", findViewById<Spinner>(R.id.units1).selectedItemPosition)
        editor.putInt("${resources.getString(R.string.notation_btn)}units2", findViewById<Spinner>(R.id.units2).selectedItemPosition)
        editor.putString("${resources.getString(R.string.notation_btn)}value1", findViewById<TextView>(R.id.value1).text.toString())
        editor.putString("${resources.getString(R.string.notation_btn)}value2", findViewById<TextView>(R.id.value2).text.toString())
        editor.putBoolean("${resources.getString(R.string.notation_btn)}value1IsSelected", isSelected(findViewById(R.id.value1)))

        editor.apply()
    }

    private fun setSavedResults(){
        val getter = getSharedPreferences("conversions", Context.MODE_PRIVATE)
        findViewById<Spinner>(R.id.units1).setSelection(getter.getInt("${resources.getString(R.string.notation_btn)}units1", 1))
        findViewById<Spinner>(R.id.units2).setSelection(getter.getInt("${resources.getString(R.string.notation_btn)}units2", 1))
        findViewById<TextView>(R.id.value1).text = getter.getString("${resources.getString(R.string.notation_btn)}value1", resources.getString(R.string.initial_operation))
        findViewById<TextView>(R.id.value2).text = getter.getString("${resources.getString(R.string.notation_btn)}value2", resources.getString(R.string.initial_operation))
        if(getter.getBoolean("${resources.getString(R.string.notation_btn)}value1IsSelected", true)) selectValue1() else selectValue2()
    }


    private fun setOnClicks(){
        val units1 = findViewById<Spinner>(R.id.units1)
        val units2 = findViewById<Spinner>(R.id.units2)
        val value1 = findViewById<EditText>(R.id.value1)
        val value2 = findViewById<EditText>(R.id.value2)
        val unit1 = findViewById<TextView>(R.id.unit1)
        val unit2 = findViewById<TextView>(R.id.unit2)
        findViewById<ImageButton>(R.id.return_btn).setOnClickListener{onBackPressedDispatcher.onBackPressed()}

        selectValue1()
        value1.setOnFocusChangeListener { _, hasFocus -> if(hasFocus){ selectValue1() } }
        value2.setOnFocusChangeListener { _, hasFocus -> if(hasFocus){ selectValue2() } }

        value1.doAfterTextChanged {
            filterInput(value1, it)
            if(value1.hasFocus()) {
                showConversion()
            }
        }
        value2.doAfterTextChanged {
            filterInput(value2, it)
            if(value2.hasFocus()) {
                showConversion()
            }
        }

        value1.filters += InputFilter { source, start, end, _, _, _ ->
            if(end-start > resources.getInteger(R.integer.max_chars_in_line)){return@InputFilter resources.getString(R.string.tooLargeNumberError)}
            for (i in start until end) { if (!data.getObjectWithName(findViewById<Spinner>(R.id.units1).selectedItem as String).getString("allowed_chars").contains(source[i])) { return@InputFilter "" } }
            null
        }
        value2.filters += InputFilter { source, start, end, _, _, _ ->
            if(end-start > resources.getInteger(R.integer.max_chars_in_line)){return@InputFilter resources.getString(R.string.tooLargeNumberError)}
            for (i in start until end) { if (!data.getObjectWithName(findViewById<Spinner>(R.id.units2).selectedItem as String).getString("allowed_chars").contains(source[i])) { return@InputFilter "" } }
            null
        }

        val adapter = ArrayAdapter(this, R.layout.drop_down_list_item,  data.getValues("name"))

        units1.adapter = adapter
        units2.adapter = adapter
        units1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(view == null){return}
                (view as TextView).text = data.getJSONObject(position).getString("name")
                unit1.text = data.getJSONObject(position).getString("shortened")
                validateValue(value1, units1)
                showConversion()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        units2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(view == null){return}
                (view as TextView).text = data.getJSONObject(position).getString("name")
                unit2.text = data.getJSONObject(position).getString("shortened")
                validateValue(value1, units2)
                showConversion()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun validateValue(value: EditText, units: Spinner){
        val text = value.text.toString()
        var result = ""
        val max = data.getObjectWithName(units.selectedItem as String).getString("allowed_chars").last()
        for(char in text){
            result += minOf(char, max)
        }
        value.setText(result)
    }

    private fun selectValue1(){
        findViewById<EditText>(R.id.value1).setTextColor(getColorFromAttr(theme, androidx.appcompat.R.attr.colorAccent))
        findViewById<EditText>(R.id.value2).setTextColor(getColorFromAttr(theme, R.attr.textColorSecondary))
    }
    private fun selectValue2(){
        findViewById<EditText>(R.id.value2).setTextColor(getColorFromAttr(theme, androidx.appcompat.R.attr.colorAccent))
        findViewById<EditText>(R.id.value1).setTextColor(getColorFromAttr(theme, R.attr.textColorSecondary))
    }

    private fun isSelected(unit: EditText): Boolean{
        return unit.currentTextColor == getColorFromAttr(theme, androidx.appcompat.R.attr.colorAccent)
    }

    private fun selectedValue():EditText = if(isSelected(findViewById(R.id.value1))) findViewById(R.id.value1) else findViewById(R.id.value2)
    private fun unSelectedValue():EditText = if(isSelected(findViewById(R.id.value1))) findViewById(R.id.value2) else findViewById(R.id.value1)
    private fun selectedUnit():String = if(isSelected(findViewById(R.id.value1))) findViewById<Spinner>(R.id.units1).selectedItem as String else findViewById<Spinner>(R.id.units2).selectedItem as String
    private fun unSelectedUnit():String = if(isSelected(findViewById(R.id.value2))) findViewById<Spinner>(R.id.units1).selectedItem as String else findViewById<Spinner>(R.id.units2).selectedItem as String

    private fun filterInput(editText: EditText, it: Editable?){
        if(it.isNullOrEmpty()){return}
        if(it.count { it == '.' } + it.count{ it == ',' } > 1){ editText.text = it.delete(it.lastIndexOf('.').coerceAtLeast(it.lastIndexOf(',')), it.lastIndexOf('.').coerceAtLeast(it.lastIndexOf(','))+1); editText.setSelection(editText.length()) }
        if(it.count { it == '-' } > 1){ editText.text = it.delete(it.lastIndexOf('-'), it.lastIndexOf('-')+1); editText.setSelection(editText.length()) }
        if(it.first() == '.' || it.first() == ','){
            it.insert(0, "0")
        }
        if(it.indexOf('-') > 0){
            editText.setText(it.replace("-".toRegex(), ""))
            editText.setSelection(editText.length())
        }
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

    private fun Char.toNumber(): Int{
        if(this.isDigit()) return this.digitToInt()
        return this.lowercaseChar() - 'a' + 10
    }
    private fun Int.toSign(): Char{
        if(this <= 9) return this.digitToChar()
        return 'a' + this - 10
    }

    private fun toDecimal(input: String): Double{
        val text = input.removePrefix("-")
        val periodIndex = text.indexOf('.').coerceAtLeast(text.indexOf(','))
        val size = if(periodIndex == -1) text.length else periodIndex
        var index = size-1
        var result = 0.0
        for(char in text){
            if(char == '.' || char == ','){continue}
            result += char.toNumber() * data.getObjectWithName(selectedUnit()).getInt("coefficient")
                .toDouble().pow(index.toDouble())
            index --
        }
        if(input.isNotEmpty() && input.first() == '-') { result = -result }
        return result
    }

    private fun fromDecimal(decimal:Double): String{
        val accuracy = getSharedPreferences(resources.getString(R.string.settings_menuitem), MODE_PRIVATE).getInt(resources.getString(R.string.accuracy_setting), resources.getInteger(R.integer.accuracy))
        val base = BigInteger.valueOf(data.getObjectWithName(unSelectedUnit()).getInt("coefficient").toLong())
        var toDivide = BigDecimal.valueOf(abs(decimal) * base.toDouble().pow(accuracy.toDouble())).toBigInteger()
        var result = ""
        while (toDivide.compareTo(BigInteger.valueOf(0)) == 1){
            result = "${toDivide.mod(base).toInt().toSign()}$result"
            if(result.length == accuracy){result = "${getPeriod(this)}$result"}
            toDivide = toDivide.divide(base)
        }
        while(result.isNotEmpty() && result.last() == '0'){
            result = result.dropLast(1)
        }
        if(decimal < 0){result = "${resources.getString(R.string.minus_btn)}$result"}
        return result.removeSuffix(getPeriod(this))
    }

    private fun showConversion(){
        unSelectedValue().setText(fromDecimal(toDecimal(selectedValue().text.toString())))
    }

}


