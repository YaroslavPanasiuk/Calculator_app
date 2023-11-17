package com.calculatorPackage.calculator

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.GridLayout.LayoutParams
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.animation.doOnEnd
import androidx.core.widget.doAfterTextChanged
import androidx.gridlayout.widget.GridLayout
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.material.color.MaterialColors
import org.apache.commons.math3.special.Gamma
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        setOnClicks()
        showPreviousResults()
        onConfigurationChanged(resources.configuration)
        scrollDown()
        setOperation()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setTheme() {
        if(themeIsDark()) {
            setTheme(R.style.Theme_CalculatorDark)
        } else{
            setTheme(R.style.Theme_Calculator)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        onStop()
    }
    override fun onStop() {
        super.onStop()
        val editor = getSharedPreferences(resources.getString(R.string.prefs_name), MODE_PRIVATE).edit()
        editor.putString("operation", findViewById<TextView>(R.id.currentOperationTextView).text.toString())
        editor.putString("result", findViewById<TextView>(R.id.resultTextView).text.toString())
        editor.putString("extended", findViewById<ImageButton>(R.id.extend_btn).tag.toString())
        editor.apply()
    }

    private fun inverseTag(tag: String): String{
        return if (tag != resources.getString(R.string.extend_btn_opened)) resources.getString(R.string.extend_btn_opened)
               else resources.getString(R.string.extend_btn_closed)
    }

    private fun setOperation(){
        val prefs = getSharedPreferences(resources.getString(R.string.prefs_name), MODE_PRIVATE)
        val operationText = prefs.getString("operation", resources.getString(R.string.initial_operation))
        val resultText = prefs.getString("result", resources.getString(R.string.initial_operation))
        val tag = prefs.getString("extended", resources.getString(R.string.extend_btn_opened))
        findViewById<ImageButton>(R.id.extend_btn).tag = inverseTag(tag.toString())
        toggleGridLayout()
        findViewById<TextView>(R.id.currentOperationTextView).text = operationText
        findViewById<TextView>(R.id.resultTextView).text = resultText
        toggleResultVisibility()
    }

    private fun themeIsDark():Boolean{
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
    }

    private fun setOnClicks(){
        val buttonClear = findViewById<View>(R.id.buttonClear) as Button
        val buttonDelete = findViewById<ImageButton>(R.id.buttonDelete)
        val buttonPeriod = findViewById<View>(R.id.buttonPeriod) as Button
        val buttonBrackets = findViewById<Button>(R.id.buttonBrackets)
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
        val buttonExtend = findViewById<ImageButton>(R.id.extend_btn)
        val buttonLg = findViewById<Button>(R.id.button_lg)
        val buttonLn = findViewById<Button>(R.id.button_ln)
        val buttonSin = findViewById<Button>(R.id.button_sin)
        val buttonCos = findViewById<Button>(R.id.button_cos)
        val buttonTan = findViewById<Button>(R.id.button_tan)
        val buttonRaiseToPower = findViewById<Button>(R.id.button_raise_to_power)
        val buttonFactorial = findViewById<Button>(R.id.button_factorial)
        val buttonSqrt = findViewById<Button>(R.id.button_sqrt)
        val buttonPi = findViewById<Button>(R.id.button_pi)
        val buttonE = findViewById<Button>(R.id.button_e)
        val resultTextView = findViewById<View>(R.id.resultTextView) as TextView
        val previousResultsTextView = findViewById<View>(R.id.previousResultsTextView) as TextView
        val previousResultsScrollView = findViewById<ScrollView>(R.id.previousResultsScrollView)
        val currantOperationTextView = findViewById<View>(R.id.currentOperationTextView) as TextView
        val grid = findViewById<androidx.gridlayout.widget.GridLayout>(R.id.grid)
        val screen = findViewById<LinearLayout>(R.id.screen)

        currantOperationTextView.setAutoSizeTextTypeUniformWithConfiguration(
            resources.getDimension(R.dimen.min_text_size).toInt(),
            resources.getDimension(R.dimen.big_text_size).toInt(),
            resources.getDimension(R.dimen.text_step).toInt(),
            TypedValue.COMPLEX_UNIT_PX
        )

        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleListener())

        previousResultsTextView.doAfterTextChanged { scrollDown() }

        buttonClear.setOnClickListener {
            if(buttonClear.text == resources.getString(R.string.clear_all_btn)){ clearAll() }
            else{ clear() }
            onTextChanged()
        }

        buttonExtend.setOnClickListener{
            toggleGridLayout()
        }

        buttonDelete.setOnClickListener {
            if(resultIsSelected()){showPreviousResults()}
            deleteAction()
        }
        val functionButtons = listOf(buttonSin, buttonCos, buttonTan, buttonLg, buttonLn)
        val operationalButtons = listOf(buttonDivide, buttonMultiply, buttonPlus, buttonMinus)
        val numericButtons = listOf(buttonOne, buttonTwo, buttonThree, buttonFour, buttonFive,
            buttonSix, buttonSeven, buttonEight, buttonNine, buttonZero, buttonE, buttonPi)
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
        buttonRaiseToPower.setOnClickListener {
            enterSign(resources.getString(R.string.raise_to_power_sign))
            onTextChanged()
        }
        buttonSqrt.setOnClickListener {
            enterSquareRoot()
            onTextChanged()
        }
        buttonFactorial.setOnClickListener {
            enterFactorial()
            onTextChanged()
        }
        functionButtons.forEach{button ->
            button.setOnClickListener {
                enterFunction(button.text.toString())
                onTextChanged()
            }
        }
        buttonBrackets.setOnClickListener {
            enterBracket()
            onTextChanged()
        }

        buttonPeriod.setOnClickListener {
            enterPeriod()
            onTextChanged()
        }
        buttonEquals.setOnClickListener {
            currantOperationTextView.text = validatedOperation()
            when{
                currantOperationTextView.text.toString().toDoubleOrNull() != null -> {return@setOnClickListener}
                resultIsSelected() -> {return@setOnClickListener}
            }
            selectResult()
            showResult()
            writeOperation()
        }

        unselectResult()
    }

    private fun toggleResultVisibility(){
        val resultTextView = findViewById<TextView>(R.id.resultTextView)
        val operation = findViewById<TextView>(R.id.currentOperationTextView).text
        val maxWeight = 0.1f
        val minWeight = 0f
        when {
            operation.isNullOrEmpty() || operation.toString() == resources.getString(R.string.initial_operation) -> {
                if((resultTextView.layoutParams as LinearLayout.LayoutParams).weight == minWeight){return}
                val animator = ValueAnimator.ofFloat(maxWeight, minWeight)
                animator.duration = resources.getInteger(R.integer.animation_duration_medium).toLong()
                animator.addUpdateListener { valueAnimator ->
                    val animatedValue = valueAnimator.animatedValue as Float
                    resultTextView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, animatedValue)
                }
                animator.start()
            }
            (resultTextView.layoutParams as LinearLayout.LayoutParams).weight == minWeight -> {
                val animator = ValueAnimator.ofFloat(minWeight, maxWeight)
                animator.duration = resources.getInteger(R.integer.animation_duration_medium).toLong()
                animator.addUpdateListener { valueAnimator ->
                    val animatedValue = valueAnimator.animatedValue as Float
                    resultTextView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, animatedValue)
                }
                animator.start()
                animator.doOnEnd { scrollDown() }
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun toggleGridLayout(){
        val buttonExtend = findViewById<ImageButton>(R.id.extend_btn)
        val firstButton = findViewById<Button>(R.id.button_lg)
        val buttonTopLeft = findViewById<Button>(R.id.buttonClear)
        val buttonTopRight = findViewById<Button>(R.id.buttonDivide)
        when(buttonExtend.tag){
            resources.getString(R.string.extend_btn_closed) -> {
                buttonExtend.setImageResource(R.drawable.baseline_close_fullscreen_24)
                buttonExtend.tag = resources.getString(R.string.extend_btn_opened)
                buttonExtend.background = resources.getDrawable(R.drawable.bottom_left_cornered_button, theme)
                buttonTopLeft.background = resources.getDrawable(R.drawable.top_left_cornered_button, theme)
                buttonTopRight.background = resources.getDrawable(R.drawable.top_right_cornered_button, theme)

                val extendAnimator = ValueAnimator.ofFloat(0f, 1f)
                extendAnimator.duration = resources.getInteger(R.integer.animation_duration_short).toLong()
                extendAnimator.addUpdateListener { valueAnimator ->
                    val animatedValue = valueAnimator.animatedValue as Float
                    firstButton.layoutParams = GridLayout.LayoutParams(
                        GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, animatedValue),
                        GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, animatedValue)
                    ).apply { width = 0; height = 0 }
                }
                extendAnimator.start()
            }
            resources.getString(R.string.extend_btn_opened) -> {
                buttonExtend.setImageResource(R.drawable.baseline_open_in_full_24)
                buttonExtend.tag = resources.getString(R.string.extend_btn_closed)
                buttonExtend.background = resources.getDrawable(R.drawable.primary_background, theme)
                buttonTopLeft.background = resources.getDrawable(R.drawable.primary_background, theme)
                buttonTopRight.background = resources.getDrawable(R.drawable.primary_background, theme)

                val collapseAnimator = ValueAnimator.ofFloat(1f, 0f)
                collapseAnimator.duration = resources.getInteger(R.integer.animation_duration_short).toLong()
                collapseAnimator.addUpdateListener { valueAnimator ->
                    val animatedValue = valueAnimator.animatedValue as Float
                    firstButton.layoutParams = GridLayout.LayoutParams(
                        GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, animatedValue),
                        GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, animatedValue)
                    ).apply { width = 0; height = 0 }
                }
                collapseAnimator.start()
            }
            else -> { Toast.makeText(applicationContext, buttonExtend.tag.toString(), Toast.LENGTH_LONG).show() }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        when(newConfig.orientation){
            ORIENTATION_LANDSCAPE -> {
                findViewById<LinearLayout>(R.id.screen).orientation = LinearLayout.HORIZONTAL
                findViewById<LinearLayout>(R.id.display).layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                findViewById<GridLayout>(R.id.grid).layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                findViewById<View>(R.id.separator).layoutParams = LinearLayout.LayoutParams(1, LayoutParams.MATCH_PARENT)
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                scrollDown()
                findViewById<LinearLayout>(R.id.screen).orientation = LinearLayout.VERTICAL
                findViewById<LinearLayout>(R.id.display).layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
                findViewById<GridLayout>(R.id.grid).layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
                findViewById<View>(R.id.separator).layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 1)
            }
            else -> {}
        }
    }

    private fun scrollDown(){
        val previousResultsScrollView = findViewById<ScrollView>(R.id.previousResultsScrollView)
        previousResultsScrollView.post { previousResultsScrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun getResult():String{
        val resultTextView = findViewById<TextView>(R.id.resultTextView)
        return resultTextView.text.split(resources.getString(R.string.equals_btn)+resources.getString(R.string.delimiter)).last()
    }

    private fun onTextChanged(text:CharSequence? = findViewById<TextView>(R.id.currentOperationTextView).text.toString()){
        scrollDown()
        toggleResultVisibility()
        if(text.isNullOrEmpty() || text == resources.getString(R.string.initial_operation)){
            findViewById<Button>(R.id.buttonClear).text = resources.getString(R.string.clear_all_btn)
        } else {
            findViewById<Button>(R.id.buttonClear).text = resources.getString(R.string.clear_btn)
        }
        if(text.isNullOrEmpty()){
            findViewById<TextView>(R.id.currentOperationTextView).text = resources.getString(R.string.initial_operation)
        }
        if(resultIsSelected()){
            unselectResult()
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
        val openBracket = resources.getString(R.string.open_bracket).last()
        val closeBracket = resources.getString(R.string.close_bracket).last()
        val constants = resources.getStringArray(R.array.constants)
        while (!(result.isEmpty() || isNumber(result.last()) || result.last().toString() == resources.getString(R.string.factorial_sign))){result = result.dropLast(1)}
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
        val operations = file.readText().lines().toMutableList()
        while (operations.size >= resources.getInteger(R.integer.results_rows)){
            operations.removeFirst()
        }
        operations.add("${validatedOperation(currantOperationTextView.text.toString())}${resources.getString(
            R.string.delimiter
        )}${resultTextView.text}")
        file.writeText(operations.joinToString ("\n"))
    }

    private fun deleteAction(count: Int = 1){
        unselectResult()
        val textView = findViewById<TextView>(R.id.currentOperationTextView)
        resources.getStringArray(R.array.functions).forEach {
            if(textView.text.length > it.length && textView.text.subSequence(textView.text.length-1-it.length, textView.text.length-1).toString() == it){
                textView.apply { text = text.dropLast(it.length+1) }
                return
            }
        }
        textView.apply { text = text.dropLast(count) }
        onTextChanged()
    }

    @SuppressLint("SetTextI18n")
    private fun enterNumber(number:String){
        val operation = findViewById<TextView>(R.id.currentOperationTextView)
        if(operation.text.toString() == resources.getString(R.string.initial_operation) || resultIsSelected()){operation.text = ""}
        if(operation.text.isNotEmpty() && (operation.text.last().toString() == resources.getString(R.string.close_bracket) ||
            operation.text.last().toString() == resources.getString(R.string.factorial_sign) ||
            resources.getStringArray(R.array.constants).contains(operation.text.last().toString()) ||
            (operation.text.last().isDigit() && resources.getStringArray(R.array.constants).contains(number)))){
            operation.apply { text = "$text${resources.getString(R.string.multiply_btn)}" }
        }
        operation.apply { text = "$text$number" }

    }

    @SuppressLint("RestrictedApi")
    private fun selectResult(){
        val resultTextView = findViewById<TextView>(R.id.resultTextView)
        val currentOperationTextView = findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.currentOperationTextView)
        val startSize = resources.getDimension(R.dimen.small_text_size).toInt()
        val endSize = resources.getDimension(R.dimen.big_text_size).toInt()
        val operationAnimator = ValueAnimator.ofInt(endSize, startSize)
        val resultAnimator = ValueAnimator.ofInt(startSize, endSize)

        resultTextView.setTextColor(resources.getColor(R.color.primary, theme))
        resultAnimator.duration = resources.getInteger(R.integer.animation_duration_short).toLong()
        operationAnimator.duration = resources.getInteger(R.integer.animation_duration_short).toLong()

        resultAnimator.addUpdateListener { valueAnimator ->
            val animatedValue = valueAnimator.animatedValue as Int
            resultTextView.setAutoSizeTextTypeUniformWithConfiguration(
                resources.getDimension(R.dimen.min_text_size).toInt(),
                animatedValue,
                resources.getDimension(R.dimen.text_step).toInt(),
                TypedValue.COMPLEX_UNIT_PX
            )
        }

        operationAnimator.addUpdateListener { valueAnimator ->
            val animatedValue = valueAnimator.animatedValue as Int
            currentOperationTextView.setAutoSizeTextTypeUniformWithConfiguration(
                resources.getDimension(R.dimen.min_text_size).toInt(),
                animatedValue,
                resources.getDimension(R.dimen.text_step).toInt(),
                TypedValue.COMPLEX_UNIT_PX
            )
        }

        resultAnimator.start()
        operationAnimator.start()
    }

    @SuppressLint("RestrictedApi")
    private fun unselectResult(){
        val resultTextView = findViewById<TextView>(R.id.resultTextView)
        val currentOperationTextView = findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.currentOperationTextView)
        val startSize = resources.getDimension(R.dimen.small_text_size).toInt()
        val endSize = resources.getDimension(R.dimen.big_text_size).toInt()
        val animationDuration = resources.getInteger(R.integer.animation_duration_short).toLong()
        val operationAnimator = ValueAnimator.ofInt(currentOperationTextView.autoSizeMaxTextSize, endSize)
        val resultAnimator = ValueAnimator.ofInt(resultTextView.autoSizeMaxTextSize, startSize)
        val color = MaterialColors.getColor(applicationContext, androidx.constraintlayout.widget.R.attr.textFillColor, resources.getColor(R.color.grey, theme))
        resultTextView.setTextColor(color)
        resultAnimator.duration = animationDuration
        operationAnimator.duration = animationDuration

        if(resultTextView.autoSizeMaxTextSize > startSize){
            resultAnimator.addUpdateListener { valueAnimator ->
                val animatedValue = valueAnimator.animatedValue as Int
                resultTextView.setAutoSizeTextTypeUniformWithConfiguration(
                    resources.getDimension(R.dimen.min_text_size).toInt(),
                    animatedValue,
                    resources.getDimension(R.dimen.text_step).toInt(),
                    TypedValue.COMPLEX_UNIT_PX
                )
            }
        }

        if(currentOperationTextView.autoSizeMaxTextSize < endSize){
            operationAnimator.addUpdateListener { valueAnimator ->
                val animatedValue = valueAnimator.animatedValue as Int
                currentOperationTextView.setAutoSizeTextTypeUniformWithConfiguration(
                    resources.getDimension(R.dimen.min_text_size).toInt(),
                    animatedValue,
                    resources.getDimension(R.dimen.text_step).toInt(),
                    TypedValue.COMPLEX_UNIT_PX
                )
            }
        }

        resultAnimator.start()
        operationAnimator.start()
    }

    private fun resultIsSelected(): Boolean{
        return findViewById<TextView>(R.id.resultTextView).currentTextColor == resources.getColor(R.color.primary, theme)
    }

    @SuppressLint("SetTextI18n")
    private fun showResult(){
        val resultTextView = findViewById<View>(R.id.resultTextView) as TextView
        val equals = resources.getString(R.string.equals_btn)
        val result = try{
            calculate(validatedOperation())
        } catch (exc: Exception){
            //calculate(validatedOperation())
            if(!exc.message.isNullOrEmpty() && exc is java.lang.ArithmeticException) exc.message!! else resources.getString(R.string.error)
        }
        val accuracy = resources.getInteger(R.integer.accuracy)
        resultTextView.text = "$equals${resources.getString(R.string.delimiter)}" +
                "${if(result.toDoubleOrNull() != null) String.format(Locale.ENGLISH, "%.${accuracy}f", result.toDouble()).toDouble() else result}"
    }

    @SuppressLint("SetTextI18n")
    private fun enterSign(sign:String){
        val operation = findViewById<TextView>(R.id.currentOperationTextView)
        val zero = resources.getString(R.string.initial_operation)
        if(resultIsSelected()){
            if(!resources.getStringArray(R.array.errors).contains(getResult())) {
                operation.text = "${getResult()}$sign"
                return
            }
            operation.text = ""
        }
        if(operation.text.toString() == zero){operation.text = ""}
        if(operation.text.toString().isEmpty() || operation.text.last().toString() == resources.getString(R.string.square_root_sign)){
            operation.text = when(sign) {
                resources.getString(R.string.minus_btn) -> sign
                else -> "${operation.text}$zero$sign"
            }
            return
        }
        if(resources.getStringArray(R.array.signs).contains(operation.text.last().toString()) ||
            operation.text.last().toString() == resources.getString(R.string.period_btn)){
            deleteAction()
        }
        if(sign == resources.getString(R.string.minus_btn)){
            operation.apply { text = "$text$sign" }
            return
        }
        if(operation.text.last().toString() == resources.getString(R.string.open_bracket)){return}
        operation.apply { text = "$text$sign" }
    }

    @SuppressLint("SetTextI18n")
    private fun enterFunction(function: String) {
        val operation = findViewById<TextView>(R.id.currentOperationTextView)
        if(resultIsSelected() || operation.text.toString() == resources.getString(R.string.initial_operation)){ operation.text = "" }
        if(operation.text.isNullOrEmpty()){
            operation.text = "$function${resources.getString(R.string.open_bracket)}"
            return
        }
        if(operation.text.last().toString() == resources.getString(R.string.period_btn)){return}
        if(resources.getStringArray(R.array.signs).contains(operation.text.last().toString()) ||
            operation.text.last().toString() == resources.getString(R.string.open_bracket)){
            operation.apply { text = "$text$function${resources.getString(R.string.open_bracket)}"}
            return
        }
        operation.apply { text = "$text${resources.getString(R.string.multiply_btn)}$function${resources.getString(R.string.open_bracket)}" }
    }

    @SuppressLint("SetTextI18n")
    private fun enterSquareRoot(){
        val operation = findViewById<TextView>(R.id.currentOperationTextView)
        val root = resources.getString(R.string.square_root_sign)
        if(resultIsSelected()){
            if(!resources.getStringArray(R.array.errors).contains(getResult())) {
                operation.text = "$root${resources.getString(R.string.open_bracket)}${getResult()}${resources.getString(R.string.close_bracket)}"
                return
            }
            operation.text = ""
        }
        if(operation.text.isNullOrEmpty() || operation.text.toString() == resources.getString(R.string.initial_operation)){
            operation.text = root
            return
        }
        if(operation.text.last().toString() == resources.getString(R.string.period_btn) || operation.text.last().toString() == root){return}
        if(resources.getStringArray(R.array.signs).contains(operation.text.last().toString()) ||
            operation.text.last().toString() == resources.getString(R.string.open_bracket)){
            operation.apply { text = "$text$root"}
            return
        }
        operation.apply { text = "$text${resources.getString(R.string.multiply_btn)}$root" }
    }

    @SuppressLint("SetTextI18n")
    private fun enterFactorial(){
        val operation = findViewById<TextView>(R.id.currentOperationTextView)
        val factorial = resources.getString(R.string.factorial_sign)
        if(resultIsSelected()){
            operation.text = "${getResult()}$factorial"
            return
        }
        if(operation.text.isNullOrEmpty()){return}
        if(operation.text.length > 1 && resources.getStringArray(R.array.signs).contains(operation.text.last().toString())){
            deleteAction()
        }
        if(isNumber(operation.text.last()) || operation.text.last().toString() == resources.getString(R.string.close_bracket)){
            operation.text = "${operation.text}$factorial"
        }
    }

    private fun isNumber(char:Char):Boolean{
        return char.isDigit() || resources.getStringArray(R.array.constants).contains(char.toString())
    }

    @SuppressLint("SetTextI18n")
    private fun enterBracket(){
        val currantOperationTextView = findViewById<View>(R.id.currentOperationTextView) as TextView
        val currentText = currantOperationTextView.text
        val openBracket = resources.getString(R.string.open_bracket)
        val closeBracket = resources.getString(R.string.close_bracket)
        val multiply = resources.getString(R.string.multiply_btn)
        if(currentText.isEmpty() || currentText.toString() == resources.getString(R.string.initial_operation)){
            currantOperationTextView.text = openBracket
            return
        }
        if(currentText.last().toString() == resources.getString(R.string.period_btn)){ return }
        currantOperationTextView.apply { text = when{
            resources.getStringArray(R.array.signs).contains(currentText.last().toString()) -> "$text$openBracket"
            currentText.last().toString() == openBracket || currentText.last().toString() == resources.getString(R.string.square_root_sign) -> "$text$openBracket"
            currentText.count { it.toString() == openBracket } <= currentText.count { it.toString() == closeBracket } -> when {
                isNumber(currentText.last()) -> "$text$multiply$openBracket"
                else -> "$text$openBracket"
            }
            else -> "$text${resources.getString(R.string.close_bracket)}"
        } }

    }

    @SuppressLint("SetTextI18n")
    private fun enterPeriod(){
        val currantOperationTextView = findViewById<View>(R.id.currentOperationTextView) as TextView
        val period = resources.getString(R.string.period_btn).first()
        if(resultIsSelected()){ currantOperationTextView.text = "" }
        val currentText = currantOperationTextView.text
        if(currentText.isEmpty() || resources.getStringArray(R.array.signs).contains(currentText.last().toString())){
            currantOperationTextView.apply { text = "$text${resources.getString(R.string.initial_operation)}$period" }
            return
        }
        if(!currentText.last().isDigit()){ return }
        var signIndex = -1
        resources.getStringArray(R.array.signs).forEach {sign ->
            signIndex = signIndex.coerceAtLeast(currentText.lastIndexOf(sign))
        }
        if(signIndex < currentText.lastIndexOf(period)){return}
        currantOperationTextView.apply { text = "$text$period" }
    }

    private fun calculate(text: String): String {
        val openBracket = resources.getString(R.string.open_bracket).first()
        val closeBracket = resources.getString(R.string.close_bracket).first()
        if(!text.contains(openBracket)){
            try { return calculateSimplified(validatedOperation(text)) } catch (exc: Exception){ throw exc }
        }
        var simplifiedText = ""
        var i = 0
        while (i < text.length){
            if(text[i] == openBracket){
                var j = i+1
                var number = ""
                var openedBrackets = 1
                var closedBrackets = 0
                while (j < text.length && closedBrackets < openedBrackets){
                    number += text[j]
                    if(text[j] == openBracket){openedBrackets ++}
                    if(text[j] == closeBracket){closedBrackets ++}
                    j++
                }
                number = calculate(number)
                if(resources.getStringArray(R.array.errors).contains(number)){return number}
                simplifiedText += number
                i = j
                continue
            }
            simplifiedText += text[i]
            i++
        }
        return calculate(simplifiedText)
    }

    private fun Double.round(accuracy: Int = resources.getInteger(R.integer.accuracy)): Double =
        String.format(Locale.ENGLISH, "%.${resources.getInteger(R.integer.accuracy)}f", this).toDouble()

    private fun calculateSimplified(text: String): String{
        val divide = resources.getString(R.string.divide_btn)
        val multiply = resources.getString(R.string.multiply_btn)
        val minus = resources.getString(R.string.minus_btn)
        val plus = resources.getString(R.string.plus_btn)
        val pow = resources.getString(R.string.raise_to_power_sign)
        val sin = resources.getString(R.string.sine_btn)
        val cos = resources.getString(R.string.cosine_btn)
        val tangent = resources.getString(R.string.tan_btn)
        val ln = resources.getString(R.string.log_natural_btn)
        val lg = resources.getString(R.string.log_10_btn)
        val sqrt = resources.getString(R.string.square_root_sign)
        val factorial = resources.getString(R.string.factorial_sign)
        val pi = resources.getString(R.string.pi_btn)
        val e = resources.getString(R.string.e_btn)
        val numberRegex = resources.getString(R.string.double_regex)
        var operation = text

        operation = operation.replace(pi, PI.toString())
        operation = operation.replace(e, Math.E.toString())
        operation = operation.replace("$sin$numberRegex".toRegex()){sin(it.value.drop(sin.length).toDouble()).toString()}
        operation = operation.replace("$cos$numberRegex".toRegex()){cos(it.value.drop(cos.length).toDouble()).toString()}
        operation = operation.replace("$tangent$numberRegex".toRegex()){
            val num = it.value.drop(tangent.length).toDouble()
            if(tan(num) > Int.MAX_VALUE) {throw ArithmeticException(resources.getString(R.string.tanError, num.round().toString()))}
            tan(num).toString()
        }

        operation = operation.replace("$lg$numberRegex".toRegex()){
            if(it.value.drop(lg.length).toDouble() <= 0){throw ArithmeticException(resources.getString(R.string.logError))}
            log10(it.value.drop(lg.length).toDouble()).toString()
        }
        operation = operation.replace("$ln$numberRegex".toRegex()){
            if(it.value.drop(ln.length).toDouble() <= 0){throw ArithmeticException(resources.getString(R.string.logError))}
            ln(it.value.drop(ln.length).toDouble()).toString()
        }

        operation = operation.replace("$sqrt$numberRegex".toRegex()){
            val res = it.value.drop(sqrt.length).toDouble().pow(0.5)
            if (res.isNaN()){throw ArithmeticException(resources.getString(R.string.sqrtError))}
            res.toString()
        }

        operation = operation.replace("$numberRegex$factorial".toRegex()){
            val num = it.value.dropLast(factorial.length).toDouble()
            when{
                num >= 0 -> Gamma.gamma(num+1).toString()
                else -> (-Gamma.gamma(-num+1)).toString()
            }
        }

        operation = "${resources.getString(R.string.initial_operation)}$plus$operation"
        val numbers = "(\\d+\\.?\\d*)([Ee]-?\\d+)?".toRegex().findAll(operation).map { it.value.toDouble() }.toList()
        if(operation.isEmpty()){ return resources.getString(R.string.initial_operation) }
        val signs = operation.split("(\\d+\\.?\\d*)([Ee]-?\\d+)?".toRegex()).filter { it.isNotEmpty() }
        val numberStack = ArrayList<Double>()
        val signStack = ArrayList<String>()
        numberStack.add(numbers.first())
        for(i in signs.indices){
            signStack.add(signs[i])
            numberStack.add(numbers[i+1])
            if(signs[i].first().toString() == pow){
                signStack.removeLast()
                var num2 = numberStack.removeLast()
                var num1 = numberStack.removeLast()
                if(signs[i].last().toString() == minus){num2 = -num2}
                if(signStack.last().last().toString() == minus){num1 = -num1}
                val res = if(signStack.last().length == 1) abs(num1.pow(num2)) else -num1.pow(num2)
                if (res.isNaN()){throw ArithmeticException(resources.getString(R.string.powError, num1.round().toString(), num2.round().toString()))}
                numberStack.add(res)
            }
        }
        var i = 0
        while(i < signStack.size){
            if(signStack[i].first().toString() == multiply || signStack[i].first().toString() == divide){
                val sign = signStack.removeAt(i)
                var num2 = numberStack.removeAt(i)
                val num1 = numberStack.removeAt(i)
                if(sign.last().toString() == minus){num2 = -num2}
                val res = if(sign.first().toString() == multiply) num1*num2 else if(num1 == 0.0) throw ArithmeticException(resources.getString(R.string.divisionByZeroError)) else num2/num1
                numberStack.add(i, res)
                i--
            }
            i++
        }
        while(signStack.isNotEmpty()){
            val sign = signStack.removeFirst()
            val num1 = numberStack.removeFirst()
            var num2 = numberStack.first()
            if(sign.length == 2 && sign.last().toString() == minus){num2 = -num2}
            numberStack[0] = if(sign.first().toString() == plus) num1+num2 else num1-num2
        }
        val res = numberStack.first()
        if (abs(res) == Double.POSITIVE_INFINITY){throw ArithmeticException(resources.getString(R.string.tooLargeNumberError))}
        return res.toString()
    }

    @SuppressLint("SetTextI18n")
    private fun clear(){
        (findViewById<View>(R.id.resultTextView) as TextView).text = "${resources.getString(R.string.equals_btn)}${resources.getString(R.string.delimiter)}${resources.getString(R.string.initial_operation)}"
        (findViewById<View>(R.id.currentOperationTextView) as TextView).text = resources.getString(R.string.initial_operation)
    }

    @SuppressLint("SetTextI18n")
    private fun clearAll(){
        File(applicationContext.filesDir, resources.getString(R.string.previous_operations_file)).writeText("")
        showPreviousResults()
        (findViewById<View>(R.id.resultTextView) as TextView).text = "${resources.getString(R.string.equals_btn)}${resources.getString(R.string.delimiter)}${resources.getString(R.string.initial_operation)}"
        (findViewById<View>(R.id.currentOperationTextView) as TextView).text = resources.getString(R.string.initial_operation)
    }


}