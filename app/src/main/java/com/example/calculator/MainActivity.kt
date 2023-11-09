package com.example.calculator

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import java.io.File
import java.util.Locale


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        setOnClicks()
        showPreviousResults()
    }

    private fun setTheme() {
        if(themeIsDark()) {
            setTheme(R.style.Theme_CalculatorDark)
        } else{
            setTheme(R.style.Theme_Calculator)
        }
    }

    private fun themeIsDark():Boolean{
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
    }

    private fun setOnClicks(){
        val buttonClear = findViewById<View>(R.id.buttonClear) as Button
        val buttonDelete = findViewById<ImageButton>(R.id.buttonDelete)
        val buttonPeriod = findViewById<View>(R.id.buttonPeriod) as Button
        val buttonOpenBracket = findViewById<View>(R.id.buttonOpenBracket) as Button
        val buttonCloseBracket = findViewById<View>(R.id.buttonCloseBracket) as Button
        val buttonDivide = findViewById<View>(R.id.buttonDivide) as Button
        val buttonMultiply = findViewById<View>(R.id.buttonMultiply) as Button
        val buttonMinus = findViewById<View>(R.id.buttonMinus) as Button
        val buttonPlus = findViewById<View>(R.id.buttonPlus) as Button
        val buttonEquals = findViewById<View>(R.id.buttonEquals) as Button
        val buttonOne = findViewById<View>(R.id.buttonOne) as Button
        val buttonTwo = findViewById<View>(R.id.buttonTwo) as Button
        val buttonThree = findViewById<View>(R.id.buttonThree) as Button
        val buttonFour = findViewById<View>(R.id.buttonFour) as Button
        val buttonFive = findViewById<View>(R.id.buttonFive) as Button
        val buttonSix = findViewById<View>(R.id.buttonSix) as Button
        val buttonSeven = findViewById<View>(R.id.buttonSeven) as Button
        val buttonEight = findViewById<View>(R.id.buttonEight) as Button
        val buttonNine = findViewById<View>(R.id.buttonNine) as Button
        val buttonZero = findViewById<View>(R.id.buttonZero) as Button
        val resultTextView = findViewById<View>(R.id.resultTextView) as TextView
        val previousResultsTextView = findViewById<View>(R.id.previousResultsTextView) as TextView
        val previousResultsScrollView = findViewById<ScrollView>(R.id.previousResultsScrollView)
        val currantOperationTextView = findViewById<View>(R.id.currentOperationTextView) as TextView
        val grid = findViewById<androidx.gridlayout.widget.GridLayout>(R.id.grid)
        val screen = findViewById<LinearLayout>(R.id.screen)

        scrollDown()
        previousResultsTextView.doAfterTextChanged { scrollDown() }

        buttonClear.setOnClickListener {
            clear()
            onTextChanged()
        }
        buttonDelete.setOnClickListener {
            deleteAction()
            onTextChanged()
        }
        val operationalButtons = listOf(buttonDivide, buttonMultiply, buttonPlus, buttonMinus)
        val numericButtons = listOf(buttonOne, buttonTwo, buttonThree, buttonFour, buttonFive,
            buttonSix, buttonSeven, buttonEight, buttonNine, buttonZero)
        numericButtons.forEach{button ->
            button.setOnClickListener {
                enterNumber(button.text.toString())
                onTextChanged()
            }
        }
        operationalButtons.forEach{button ->
            button.setOnClickListener {
                enterSign(button.text.toString())
                onTextChanged()
            }
        }
        buttonOpenBracket.setOnClickListener {
            enterOpenBracket()
            onTextChanged()
        }
        buttonCloseBracket.setOnClickListener { enterCloseBracket() }

        buttonPeriod.setOnClickListener {
            enterPeriod()
            onTextChanged()
        }
        currantOperationTextView.doAfterTextChanged {
            resultTextView.visibility = if(it.isNullOrEmpty() || it.toString() == resources.getString(R.string.initial_operation)) View.GONE else View.VISIBLE
        }
        buttonEquals.setOnClickListener {
            currantOperationTextView.text = validatedOperation()
            if(currantOperationTextView.text.toString().toDoubleOrNull() == getResult().toDoubleOrNull() || resultIsSelected()){ return@setOnClickListener }
            selectResult()
            showResult()
            writeOperation()
        }
        unselectResult()
    }

    private fun scrollDown(){
        val previousResultsScrollView = findViewById<ScrollView>(R.id.previousResultsScrollView)
        previousResultsScrollView.post { previousResultsScrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun getResult():String{
        val resultTextView = findViewById<TextView>(R.id.resultTextView)
        return resultTextView.text.split(resources.getString(R.string.delimiter)).last()
    }

    private fun onTextChanged(text:CharSequence? = findViewById<TextView>(R.id.currentOperationTextView).text){
        scrollDown()
        if(resultIsSelected()){
            unselectResult()
            if(text.isNullOrEmpty() || getSigns().contains(text.last().toString())){
                showPreviousResults()
                showResult()
                return
            }
            clear()
            findViewById<TextView>(R.id.currentOperationTextView).text = when(text.last()){
                resources.getString(R.string.period_btn).last() -> "${resources.getString(R.string.initial_operation)}${text.last()}"
                else -> text.last().toString()
            }
            showPreviousResults()
        }
        showResult()
    }

    private fun showPreviousResults() {
        val file = File(applicationContext.filesDir, resources.getString(R.string.previous_operations_file))
        if(file.exists()){ findViewById<TextView>(R.id.previousResultsTextView).text = file.readText() }
    }

    private fun validatedOperation(text:String = findViewById<TextView>(R.id.currentOperationTextView).text.toString()): String {
        var result = text
        val openBracket = (findViewById<View>(R.id.buttonOpenBracket) as TextView).text.last()
        val closeBracket = (findViewById<View>(R.id.buttonCloseBracket) as TextView).text.last()
        while (!(result.isEmpty() || result.last().isDigit())){result = result.dropLast(1)}
        if(result.isEmpty() || result == resources.getString(R.string.minus_btn)){
            return resources.getText(R.string.initial_operation).toString()
        }
        val bracketsToAdd = result.count{it == openBracket} - result.count{it == closeBracket}
        for(i in 0 until bracketsToAdd){
            result = "$result$closeBracket"
        }
        return result
    }

    private fun writeOperation(){
        val currantOperationTextView = findViewById<View>(R.id.currentOperationTextView) as TextView
        val resultTextView = findViewById<View>(R.id.resultTextView) as TextView
        val file = File(applicationContext.filesDir, resources.getString(R.string.previous_operations_file))
        if(!file.exists()){
            file.createNewFile()
        }
        val operations = file.readText().lines().zipWithNext {a, b -> a + "\n" + b}.filterIndexed { index, _ -> index % 2 == 0 }.toMutableList()
        while (operations.size >= resources.getInteger(R.integer.results_rows)/2){
            operations.removeFirst()
        }
        operations.add("${validatedOperation(currantOperationTextView.text.toString())}\n${resultTextView.text}")
        file.writeText(operations.joinToString ("\n"))
    }

    private fun deleteAction(count: Int = 1){
        unselectResult()
        (findViewById<View>(R.id.currentOperationTextView) as TextView).apply { text = text.dropLast(count) }
    }

    @SuppressLint("SetTextI18n")
    private fun enterNumber(number:String){
        val currantOperationTextView = findViewById<View>(R.id.currentOperationTextView) as TextView
        val currentText = currantOperationTextView.text.toString()
        if(currentText == resources.getString(R.string.initial_operation)){currantOperationTextView.text = ""}
        if(currentText.isNotEmpty() && currentText.last() == resources.getString(R.string.close_bracket_btn).first()){
            currantOperationTextView.apply { text = "$text${resources.getString(R.string.multiply_btn)}" }
        }
        currantOperationTextView.apply { text = "$text$number" }

    }

    @SuppressLint("RestrictedApi")
    private fun selectResult(){
        val resultTextView = findViewById<TextView>(R.id.resultTextView)
        val currentOperationTextView = findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.currentOperationTextView)
        resultTextView.setTextColor(resources.getColor(R.color.primary, theme))
        resultTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.big_text_size))
        currentOperationTextView.setAutoSizeTextTypeUniformWithConfiguration(
            resources.getDimension(R.dimen.min_text_size).toInt(),
            resources.getDimension(R.dimen.small_text_size).toInt(),
            resources.getDimension(R.dimen.text_step).toInt(),
            TypedValue.COMPLEX_UNIT_PX
        )
    }

    private fun unselectResult(){
        val resultTextView = findViewById<TextView>(R.id.resultTextView)
        val currentOperationTextView = findViewById<TextView>(R.id.currentOperationTextView)
        resultTextView.setTextColor(getColor(R.color.grey))
        resultTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.small_text_size))
        currentOperationTextView.setAutoSizeTextTypeUniformWithConfiguration(
            resources.getDimension(R.dimen.min_text_size).toInt(),
            resources.getDimension(R.dimen.big_text_size).toInt(),
            resources.getDimension(R.dimen.text_step).toInt(),
            TypedValue.COMPLEX_UNIT_PX
        )
    }

    private fun resultIsSelected(): Boolean{
        return findViewById<TextView>(R.id.resultTextView).currentTextColor == resources.getColor(R.color.primary, theme)
    }

    private fun getSigns(): List<String>{
        return listOf(
            resources.getString(R.string.divide_btn),
            resources.getString(R.string.multiply_btn),
            resources.getString(R.string.minus_btn),
            resources.getString(R.string.plus_btn)
        )
    }

    @SuppressLint("SetTextI18n")
    private fun showResult(){
        val resultTextView = findViewById<View>(R.id.resultTextView) as TextView
        val equals = resources.getString(R.string.equals_btn)
        val result = calculate(validatedOperation()).toDouble()
        val accuracy = resources.getInteger(R.integer.accuracy)
        resultTextView.text = "$equals${resources.getString(R.string.delimiter)}${String.format(Locale.ENGLISH, "%.${accuracy}f", result).toDouble()}"
    }

    @SuppressLint("SetTextI18n")
    private fun enterSign(sign:String){
        val currantOperationTextView = findViewById<View>(R.id.currentOperationTextView) as TextView
        val currentText = currantOperationTextView.text
        if(resultIsSelected()){
            currantOperationTextView.text = "${getResult()}$sign"
            return
        }
        if(currentText.isEmpty() || currentText.toString() == resources.getString(R.string.initial_operation)){
            if(sign == resources.getString(R.string.minus_btn)) {
                currantOperationTextView.apply { text = sign }
            }
            return
        }
        if(getSigns().contains(currentText.last().toString()) ||
            currentText.last().toString() == resources.getString(R.string.period_btn)){
            deleteAction()
        }
        if(sign == resources.getString(R.string.minus_btn)){
            currantOperationTextView.apply { text = "$text$sign" }
            return
        }
        if(currentText.last().toString() == resources.getString(R.string.open_bracket_btn)){return}
        currantOperationTextView.apply { text = "$text$sign" }
    }

    @SuppressLint("SetTextI18n")
    private fun enterOpenBracket(){
        val currantOperationTextView = findViewById<View>(R.id.currentOperationTextView) as TextView
        val currentText = currantOperationTextView.text
        val openBracket = resources.getString(R.string.open_bracket_btn)
        if(currentText.isEmpty() || currentText.toString() == resources.getString(R.string.initial_operation)){
            currantOperationTextView.text = openBracket
            return
        }
        if(currentText.last().toString() == resources.getString(R.string.period_btn)){ return }
        if(currentText.last().isDigit()){
            currantOperationTextView.apply { text = "$text${resources.getString(R.string.multiply_btn)}" }
        }
        currantOperationTextView.apply { text = "$text$openBracket" }
    }

    @SuppressLint("SetTextI18n")
    private fun enterCloseBracket(){
        val currantOperationTextView = findViewById<View>(R.id.currentOperationTextView) as TextView
        val currentText = currantOperationTextView.text
        val openBracket = resources.getString(R.string.open_bracket_btn).first()
        val closeBracket = resources.getString(R.string.close_bracket_btn).first()
        if(currentText.isEmpty() || currentText.last().toString() == resources.getString(R.string.period_btn)){ return }
        if(currentText.last() == openBracket){
            deleteAction(2)
            return
        }
        if(currentText.count { it == openBracket } > currentText.count { it == closeBracket }){
            currantOperationTextView.apply { text = "$text$closeBracket" }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun enterPeriod(){
        val currantOperationTextView = findViewById<View>(R.id.currentOperationTextView) as TextView
        val currentText = currantOperationTextView.text
        val period = resources.getString(R.string.period_btn).first()
        if(currentText.isEmpty() || resultIsSelected() || getSigns().contains(currentText.last().toString())){
            currantOperationTextView.apply { text = "$text${resources.getString(R.string.initial_operation)}$period" }
            return
        }
        if(!currentText.last().isDigit()){ return }
        var signIndex = -1
        getSigns().forEach {sign ->
            signIndex = signIndex.coerceAtLeast(currentText.lastIndexOf(sign))
        }
        if(signIndex < currentText.lastIndexOf(period)){return}
        currantOperationTextView.apply { text = "$text$period" }
    }

    private fun calculate(text: String): String {
        var result = text
        val openBracket = resources.getString(R.string.open_bracket_btn).first()
        val closeBracket = resources.getString(R.string.close_bracket_btn).first()
        if(!result.contains(openBracket)){
            return calculateSimplified(validatedOperation(result))
        }
        var simplifiedText = ""
        var i = 0
        while (i < text.length){
            if(text[i] == openBracket){
                var j = i+1
                var number = ""
                var openedBrackets = 1
                var closedBrackets = 0
                while (j < result.length && closedBrackets < openedBrackets){
                    number += result[j]
                    if(result[j] == openBracket){openedBrackets ++}
                    if(result[j] == closeBracket){closedBrackets ++}
                    j++
                }
                number = calculate(number.dropLast(1))
                simplifiedText += number
                i = j
                continue
            }
            simplifiedText += result[i]
            i++
        }
        return calculate(simplifiedText)
    }

    private fun calculateSimplified(text: String): String{
        val divide = resources.getString(R.string.divide_btn).first()
        val multiply = resources.getString(R.string.multiply_btn).first()
        val minus = resources.getString(R.string.minus_btn).first()
        val plus = resources.getString(R.string.plus_btn).first()
        val numberTexts = text.split(divide, multiply, minus, plus).filter { it.isNotEmpty() }
        val numbers = arrayListOf<Double>()
        if(text.isEmpty()){ return resources.getString(R.string.initial_operation) }
        if(text.first() == minus){ numbers.add(0.0) }
        numberTexts.forEach{ numbers.add(it.toDouble()) }
        val signs = text.split("\\d+\\.?\\d*E?\\d?".toRegex()).filter { it.isNotEmpty() }
        val numberStack = ArrayList<Double>()
        val signStack = ArrayList<String>()
        numberStack.add(numbers.first())
        for(i in signs.indices){
            signStack.add(signs[i])
            numberStack.add(numbers[i+1])
            if(signs[i].first() == divide || signs[i].first() == multiply){
                signStack.removeLast()
                var num2 = numberStack.removeLast()
                val num1 = numberStack.removeLast()
                if(signs[i].length == 2 && signs[i].last() == minus){num2 = -num2}
                numberStack.add(if (signs[i].first() == divide) num1/num2 else num1*num2)
            }
        }
        while(signStack.isNotEmpty()){
            val sign = signStack.removeFirst()
            val num1 = numberStack.removeFirst()
            var num2 = numberStack[0]
            if(sign.length == 2 && sign.last() == minus){num2 = -num2}
            numberStack[0] = if (sign.first() == minus) num1-num2 else num1+num2
        }

        return numberStack.first().toString()
    }

    @SuppressLint("SetTextI18n")
    private fun clear(){
        (findViewById<View>(R.id.resultTextView) as TextView).text = "${resources.getString(R.string.equals_btn)}${resources.getString(R.string.delimiter)}${resources.getString(R.string.initial_operation)}"
        (findViewById<View>(R.id.currentOperationTextView) as TextView).text = resources.getString(R.string.initial_operation)
    }


}