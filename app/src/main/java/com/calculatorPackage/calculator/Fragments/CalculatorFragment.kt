package com.calculatorPackage.calculator


import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Bundle
import android.text.Html
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.GridLayout.LayoutParams
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.animation.doOnEnd
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.gridlayout.widget.GridLayout
import androidx.preference.PreferenceManager
import com.calculatorPackage.calculator.SharedMethods.Companion.addNumberSeparator
import com.calculatorPackage.calculator.SharedMethods.Companion.getComma
import com.calculatorPackage.calculator.SharedMethods.Companion.getPeriod
import com.calculatorPackage.calculator.SharedMethods.Companion.validateResult
import org.apache.commons.math3.special.Gamma
import java.io.File
import java.time.LocalDate
import java.time.Period
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

class CalculatorFragment: Fragment(R.layout.fragment_calculator) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnClicks()
        setOperation()
        onConfigurationChanged(resources.configuration)
        scrollDown()
        PreferenceManager.setDefaultValues(activity as Context, R.xml.user_preferences, false)
        deleteHistoryIfNeeded()
    }

    override fun onStop() {
        super.onStop()
        saveResults()
    }

    override fun onPause() {
        super.onPause()
        saveResults()
    }



    private fun saveResults(){
        val editor = requireActivity().getSharedPreferences(resources.getString(R.string.prefs_name), Context.MODE_PRIVATE).edit()
        editor.putString("operation", requireView().findViewById<TextView>(R.id.currentOperationTextView).text.toString())
        editor.putString("result", requireView().findViewById<TextView>(R.id.resultTextView).text.toString())
        editor.putString("previousOperations", requireView().findViewById<TextView>(R.id.previousResultsTextView).text.toString())
        editor.putString("extended", requireView().findViewById<ImageButton>(R.id.extend_btn).tag.toString())
        editor.putString("units", requireView().findViewById<Button>(R.id.units_btn).text.toString())
        editor.putString("prevComma", getComma(requireActivity()))
        editor.putString("prevPeriod", getPeriod(requireActivity()))
        editor.apply()
    }

    @SuppressLint("SimpleDateFormat")
    private fun deleteHistoryIfNeeded(){
        val condition = requireActivity().getSharedPreferences(resources.getString(R.string.settings_menuitem), Context.MODE_PRIVATE)
            .getString(resources.getString(R.string.auto_delete_history), "")

        val file = File(requireActivity().filesDir, resources.getString(R.string.previous_operations_file))
        if(!file.exists()){return}
        var content = file.readLines().toMutableList()
        val size = content.size
        when(condition){
            resources.getString(R.string.after_1_week) -> {
                while(content.size > 0 && content.last().split(";;").size > 1 ){
                    val date = LocalDate.parse(content.last().split(";;")[1]).plus(Period.ofWeeks(1))
                    if(date.isAfter(LocalDate.now())){ content.removeFirstOrNull() }
                }
            }
            resources.getString(R.string.after_1_month) -> {
                while(content.size > 0 && content.last().split(";;").size > 1 ){
                    val date = LocalDate.parse(content.last().split(";;")[1]).plus(Period.ofMonths(1))
                    if(date.isAfter(LocalDate.now())){ content.removeFirstOrNull() }
                }
            }
            resources.getString(R.string.after_1_year) -> {
                while(content.size > 0 && content.last().split(";;").size > 1 ){
                    val date = LocalDate.parse(content.last().split(";;")[1]).plus(Period.ofYears(1))
                    if(date.isAfter(LocalDate.now())){ content.removeFirstOrNull() }
                }
            }
            resources.getString(R.string.after_100_records) -> {
                content = content.filterIndexed{index, _ -> content.size - index <= 100}.toMutableList()
            }
            resources.getString(R.string.after_1000_records) -> {
                content = content.filterIndexed{index, _ -> content.size - index <= 1000}.toMutableList()
            }
        }
        if(content.size < size){
            file.writeText(content.joinToString("\n"))
        }
    }

    private fun inverseTag(tag: String): String{
        return if (tag != resources.getString(R.string.extend_btn_opened)) resources.getString(R.string.extend_btn_opened)
        else resources.getString(R.string.extend_btn_closed)
    }

    private fun setOperation(){
        val prefs = requireActivity().getSharedPreferences(resources.getString(R.string.prefs_name), Context.MODE_PRIVATE)
        var operationText = prefs.getString("operation", resources.getString(R.string.initial_operation)).toString()
        val resultText = prefs.getString("result", resources.getString(R.string.initial_operation))
        val previousOperationsText = prefs.getString("previousOperations", "")
        val tag = prefs.getString("extended", resources.getString(R.string.extend_btn_opened))
        val units = prefs.getString("units", resources.getString(R.string.radians_units))
        val prevComma = prefs.getString("prevComma", getComma(requireActivity())).toString()
        val prevPeriod = prefs.getString("prevPeriod", getPeriod(requireActivity())).toString()
        requireView().findViewById<ImageButton>(R.id.extend_btn).tag = inverseTag(tag.toString())
        toggleGridLayout(0)
        requireView().findViewById<TextView>(R.id.units_btn).text = units
        requireView().findViewById<TextView>(R.id.resultTextView).text = resultText
        requireView().findViewById<TextView>(R.id.previousResultsTextView).text = previousOperationsText
        if(prevComma != getComma(requireActivity())){ operationText = operationText.replace(prevComma, "@").replace(prevPeriod, getPeriod(requireActivity())).replace("@", getComma(requireActivity())) }
        requireView().findViewById<TextView>(R.id.currentOperationTextView).text = operationText
        toggleResultVisibility()
    }

    private fun setOnClicks(){
        val buttonClear = requireView().findViewById<View>(R.id.buttonClear) as Button
        val buttonDelete = requireView().findViewById<ImageButton>(R.id.buttonDelete)
        val buttonPeriod = requireView().findViewById<View>(R.id.buttonPeriod) as Button
        val buttonBrackets = requireView().findViewById<Button>(R.id.buttonBrackets)
        val buttonDivide = requireView().findViewById<View>(R.id.buttonDivide) as Button
        val buttonMultiply = requireView().findViewById<View>(R.id.buttonMultiply) as Button
        val buttonMinus = requireView().findViewById<View>(R.id.buttonMinus) as Button
        val buttonPlus = requireView().findViewById<View>(R.id.buttonPlus) as Button
        val buttonEquals = requireView().findViewById<View>(R.id.buttonEquals) as Button
        val buttonOne = requireView().findViewById<View>(R.id.buttonOne) as Button
        val buttonTwo = requireView().findViewById<View>(R.id.buttonTwo) as Button
        val buttonThree = requireView().findViewById<View>(R.id.buttonThree) as Button
        val buttonFour = requireView().findViewById<View>(R.id.buttonFour) as Button
        val buttonFive = requireView().findViewById<View>(R.id.buttonFive) as Button
        val buttonSix = requireView().findViewById<View>(R.id.buttonSix) as Button
        val buttonSeven = requireView().findViewById<View>(R.id.buttonSeven) as Button
        val buttonEight = requireView().findViewById<View>(R.id.buttonEight) as Button
        val buttonNine = requireView().findViewById<View>(R.id.buttonNine) as Button
        val buttonZero = requireView().findViewById<View>(R.id.buttonZero) as Button
        val buttonExtend = requireView().findViewById<ImageButton>(R.id.extend_btn)
        val buttonLg = requireView().findViewById<Button>(R.id.button_lg)
        val buttonLn = requireView().findViewById<Button>(R.id.button_ln)
        val buttonSin = requireView().findViewById<Button>(R.id.button_sin)
        val buttonCos = requireView().findViewById<Button>(R.id.button_cos)
        val buttonTan = requireView().findViewById<Button>(R.id.button_tan)
        val buttonRaiseToPower = requireView().findViewById<Button>(R.id.button_raise_to_power)
        val buttonFactorial = requireView().findViewById<Button>(R.id.button_factorial)
        val buttonSqrt = requireView().findViewById<Button>(R.id.button_sqrt)
        val buttonPi = requireView().findViewById<Button>(R.id.button_pi)
        val buttonE = requireView().findViewById<Button>(R.id.button_e)
        val buttonUnits = requireView().findViewById<Button>(R.id.units_btn)
        val buttonSettings = requireView().findViewById<ImageButton>(R.id.options)
        val resultTextView = requireView().findViewById<View>(R.id.resultTextView) as TextView
        val previousResultsTextView = requireView().findViewById<View>(R.id.previousResultsTextView) as TextView
        val previousResultsScrollView = requireView().findViewById<ScrollView>(R.id.previousResultsScrollView)
        val currentOperationTextView = requireView().findViewById<View>(R.id.currentOperationTextView) as TextView
        val grid = requireView().findViewById<androidx.gridlayout.widget.GridLayout>(R.id.grid)
        val screen = requireView().findViewById<LinearLayout>(R.id.screen)
        val buttonAnswer = requireView().findViewById<Button>(R.id.button_ans)
        val buttonExp = requireView().findViewById<Button>(R.id.button_exp)
        val buttonPercents = requireView().findViewById<Button>(R.id.button_percents)
        val buttonInverse = requireView().findViewById<Button>(R.id.button_inverse)
        currentOperationTextView.setAutoSizeTextTypeUniformWithConfiguration(
            resources.getDimension(R.dimen.min_text_size).toInt(),
            resources.getDimension(R.dimen.big_text_size).toInt(),
            resources.getDimension(R.dimen.text_step).toInt(),
            TypedValue.COMPLEX_UNIT_PX
        )

        buttonPeriod.text = getPeriod(requireActivity())

        buttonUnits.setOnClickListener { toggleUnits(it as Button); showResult() }

        buttonInverse.setOnClickListener { inverseButtons() }

        buttonPercents.setOnClickListener { calculatePercents() }

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

        buttonAnswer.setOnClickListener {
            enterNumber(getCurrentResult())
            onTextChanged()
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
                enterFunction(button.tag.toString())
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

        buttonExp.setOnClickListener {
            enterExponent()
        }
        buttonEquals.setOnClickListener {
            currentOperationTextView.text = validatedOperation()
            when{
                currentOperationTextView.text.toString().toDoubleOrNull() != null -> {return@setOnClickListener}
                resultIsSelected() -> {return@setOnClickListener}
            }
            selectResult()
            showResult()
            writeOperation()
            deleteHistoryIfNeeded()
        }

        unselectResult()
    }

    private fun getCurrentResult(): String{
        val prefs = requireActivity().getSharedPreferences(resources.getString(R.string.data_btn), Context.MODE_PRIVATE)
        return prefs.getString("result", resources.getString(R.string.initial_operation)).toString()
    }

    private fun toggleResultVisibility(){
        val resultTextView = requireView().findViewById<TextView>(R.id.resultTextView)
        val operation = requireView().findViewById<TextView>(R.id.currentOperationTextView).text
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
    private fun toggleGridLayout(duration: Int = resources.getInteger(R.integer.animation_duration_short)){
        val buttonExtend = requireView().findViewById<ImageButton>(R.id.extend_btn)
        val firstButton = requireView().findViewById<Button>(R.id.button_inverse)
        val secondButton = requireView().findViewById<Button>(R.id.units_btn)
        val buttonTopLeft = requireView().findViewById<Button>(R.id.buttonClear)
        val buttonTopRight = requireView().findViewById<Button>(R.id.buttonDivide)
        when(buttonExtend.tag){
            resources.getString(R.string.extend_btn_closed) -> {
                buttonExtend.setImageResource(R.drawable.baseline_close_fullscreen_24)
                buttonExtend.tag = resources.getString(R.string.extend_btn_opened)
                buttonExtend.background = resources.getDrawable(R.drawable.bottom_left_cornered_button, requireActivity().theme)
                buttonTopLeft.background = resources.getDrawable(R.drawable.top_left_cornered_button, requireActivity().theme)
                buttonTopRight.background = resources.getDrawable(R.drawable.top_right_cornered_button, requireActivity().theme)

                val extendAnimator = ValueAnimator.ofFloat(0f, 0.9f)
                extendAnimator.duration = duration.toLong()
                extendAnimator.addUpdateListener { valueAnimator ->
                    val animatedValue = valueAnimator.animatedValue as Float
                    secondButton.layoutParams = GridLayout.LayoutParams(
                        GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, animatedValue),
                        GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, animatedValue)
                    ).apply { width = 0; height = 0 }
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
                buttonExtend.background = resources.getDrawable(R.drawable.primary_background, requireActivity().theme)
                buttonTopLeft.background = resources.getDrawable(R.drawable.primary_background, requireActivity().theme)
                buttonTopRight.background = resources.getDrawable(R.drawable.primary_background, requireActivity().theme)

                val collapseAnimator = ValueAnimator.ofFloat(0.9f, 0f)
                collapseAnimator.duration = duration.toLong()
                collapseAnimator.addUpdateListener { valueAnimator ->
                    val animatedValue = valueAnimator.animatedValue as Float
                    secondButton.layoutParams = GridLayout.LayoutParams(
                        GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, animatedValue),
                        GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, animatedValue)
                    ).apply { width = 0; height = 0 }
                    firstButton.layoutParams = GridLayout.LayoutParams(
                        GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, animatedValue),
                        GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, animatedValue)
                    ).apply { width = 0; height = 0 }
                }
                collapseAnimator.start()
            }
            else -> { Toast.makeText(requireActivity(), buttonExtend.tag.toString(), Toast.LENGTH_LONG).show() }
        }
    }

    private fun toggleUnits(button: Button){
        val deg = resources.getString(R.string.degrees_units)
        val rad = resources.getString(R.string.radians_units)
        button.text = if(button.text.toString() == deg) rad else deg
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val screen = view?.findViewById<LinearLayout>(R.id.screen)
        val display = view?.findViewById<LinearLayout>(R.id.display)
        val grid = view?.findViewById<GridLayout>(R.id.grid)
        val separator = view?.findViewById<View>(R.id.separator)
        if(screen == null || display == null || grid == null || separator == null){return}
        scrollDown()
        when(newConfig.orientation){
            ORIENTATION_LANDSCAPE -> {
                screen.orientation = LinearLayout.HORIZONTAL
                display.layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                grid.layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                separator.layoutParams = LinearLayout.LayoutParams(1, LayoutParams.MATCH_PARENT)
                screen.removeAllViews()
                screen.addView(grid)
                screen.addView(separator)
                screen.addView(display)
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                screen.orientation = LinearLayout.VERTICAL
                display.layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
                grid.layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.2f)
                separator.layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 1)
                screen.removeAllViews()
                screen.addView(display)
                screen.addView(separator)
                screen.addView(grid)
            }
            else -> {}
        }
    }

    private fun scrollDown(){
        val previousResultsScrollView = requireView().findViewById<ScrollView>(R.id.previousResultsScrollView)
        previousResultsScrollView.post { previousResultsScrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun getResult():String{
        val resultTextView = requireView().findViewById<TextView>(R.id.resultTextView)
        return resultTextView.text.split(resources.getString(R.string.equals_btn)+resources.getString(R.string.delimiter)).last()
    }

    private fun onTextChanged(){
        scrollDown()
        toggleResultVisibility()
        val operation = requireView().findViewById<TextView>(R.id.currentOperationTextView)
        operation.text = addNumberSeparator(operation.text.toString(), requireActivity())

        if(operation.text.isNullOrEmpty() || operation.text == resources.getString(R.string.initial_operation)){
            requireView().findViewById<Button>(R.id.buttonClear).text = resources.getString(R.string.clear_all_btn)
        } else {
            requireView().findViewById<Button>(R.id.buttonClear).text = resources.getString(R.string.clear_btn)
        }
        if(operation.text.isNullOrEmpty()){
            operation.text = resources.getString(R.string.initial_operation)
        }
        if(resultIsSelected()){
            unselectResult()
            showPreviousResults()
        }
        showResult()
    }

    @SuppressLint("SetTextI18n")
    private fun showPreviousResults() {
        val textView = requireView().findViewById<TextView>(R.id.previousResultsTextView)
        if(textView.tag != null){ textView.apply { text = "$text\n$tag" } }
        textView.tag = null
    }

    private fun validatedOperation(text:String = requireView().findViewById<TextView>(R.id.currentOperationTextView).text.toString()): String {
        var result = text
        val openBracket = resources.getString(R.string.open_bracket).last()
        val closeBracket = resources.getString(R.string.close_bracket).last()
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

    @SuppressLint("CommitPrefEdits")
    private fun writeOperation(){
        val currentOperationTextView = requireView().findViewById<View>(R.id.currentOperationTextView) as TextView
        val resultTextView = requireView().findViewById<View>(R.id.resultTextView) as TextView
        val file = File(requireActivity().filesDir, resources.getString(R.string.previous_operations_file))
        if(!file.exists()){ file.createNewFile() }
        val currentOperation = "${validatedOperation(currentOperationTextView.text.toString())}${resources.getString(R.string.delimiter)}${resultTextView.text}"
        requireView().findViewById<TextView>(R.id.previousResultsTextView).tag = currentOperation
        val editor = requireActivity().getSharedPreferences(resources.getString(R.string.data_btn), Context.MODE_PRIVATE).edit()
        editor.putString("result", resultTextView.text.toString().removePrefix(resources.getString(R.string.equals_btn)).removePrefix(resources.getString(R.string.delimiter)))
        editor.apply()
        file.appendText("\n$currentOperation;;${LocalDate.now()}")

    }

    private fun deleteAction(count: Int = 1){
        unselectResult()
        val textView = requireView().findViewById<TextView>(R.id.currentOperationTextView)
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
    private fun enterNumber(value:String){
        var number = value
        val operation = requireView().findViewById<TextView>(R.id.currentOperationTextView)
        if(operation.text.toString() == resources.getString(R.string.initial_operation) || resultIsSelected()){operation.text = ""}
        if(number.first().toString() == resources.getString(R.string.minus_btn)){
            number = "${resources.getString(R.string.open_bracket)}$number${resources.getString(R.string.close_bracket)}"
        }
        if(operation.text.isEmpty() ||
            resources.getStringArray(R.array.signs).contains(operation.text.last().toString()) ||
            operation.text.last().toString() == resources.getString(R.string.open_bracket) ||
            operation.text.last().toString() == resources.getString(R.string.square_root_sign) ||
            (
                    (operation.text.last().isDigit() ||
                    operation.text.last().toString() == getPeriod(requireActivity()) ||
                    operation.text.last().toString() == resources.getString(R.string.exponent)) &&
                    number.length == 1 && number.first().isDigit())
            ){
            operation.apply { text = "$text$number"}
            return
        }
        if(operation.text.last().toString() == getPeriod(requireActivity())||
            operation.text.last().toString() == resources.getString(R.string.exponent)){
            operation.apply { text = "$text${resources.getString(R.string.initial_operation)}" }
        }
        operation.apply { text = "$text${resources.getString(R.string.multiply_btn)}$number" }

    }

    @ColorInt
    fun getColorFromAttr(
        @AttrRes attrColor: Int,
        typedValue: TypedValue = TypedValue(),
        resolveRefs: Boolean = true
    ): Int {
        requireActivity().theme.resolveAttribute(attrColor, typedValue, resolveRefs)
        return typedValue.data
    }

    @SuppressLint("RestrictedApi")
    private fun selectResult(){
        val resultTextView = requireView().findViewById<TextView>(R.id.resultTextView)
        val currentOperationTextView = requireView().findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.currentOperationTextView)
        val startSize = resources.getDimension(R.dimen.small_text_size).toInt()
        val endSize = resources.getDimension(R.dimen.big_text_size).toInt()
        val operationAnimator = ValueAnimator.ofInt(endSize, startSize)
        val resultAnimator = ValueAnimator.ofInt(startSize, endSize)

        resultTextView.setTextColor(getColorFromAttr(androidx.appcompat.R.attr.colorAccent))
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
        val resultTextView = requireView().findViewById<TextView>(R.id.resultTextView)
        val currentOperationTextView = requireView().findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.currentOperationTextView)
        val startSize = resources.getDimension(R.dimen.small_text_size).toInt()
        val endSize = resources.getDimension(R.dimen.big_text_size).toInt()
        val animationDuration = resources.getInteger(R.integer.animation_duration_short).toLong()
        val operationAnimator = ValueAnimator.ofInt(currentOperationTextView.autoSizeMaxTextSize, endSize)
        val resultAnimator = ValueAnimator.ofInt(resultTextView.autoSizeMaxTextSize, startSize)
        resultTextView.setTextColor(getColorFromAttr(androidx.constraintlayout.widget.R.attr.textOutlineColor))
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
        return requireView().findViewById<TextView>(R.id.resultTextView).currentTextColor == getColorFromAttr(
            androidx.appcompat.R.attr.colorAccent
        )
    }

    @SuppressLint("SetTextI18n")
    private fun showResult(operation:String = validatedOperation()){
        val resultTextView = requireView().findViewById<View>(R.id.resultTextView) as TextView
        val equals = resources.getString(R.string.equals_btn)
        val result = try{
            calculate(operation)
        } catch (exc: Exception){
            //calculate(validatedOperation())
            if(!exc.message.isNullOrEmpty() && exc is java.lang.ArithmeticException) exc.message!! else resources.getString(R.string.error)
        }
        resultTextView.text = "$equals${resources.getString(R.string.delimiter)}${validateResult(result, requireActivity())}"
    }

    @SuppressLint("SetTextI18n")
    private fun enterSign(sign:String){
        val operation = requireView().findViewById<TextView>(R.id.currentOperationTextView)
        val zero = resources.getString(R.string.initial_operation)
        if(resultIsSelected()){
            if(!resources.getStringArray(R.array.errors).contains(getResult())) {
                operation.text = "${getResult()}$sign"
                return
            }
            operation.text = ""
        }
        if(operation.text.toString() == zero){operation.text = ""}
        if(operation.text.toString().isEmpty() || operation.text.last().toString() == resources.getString(R.string.square_root_sign) || operation.text.last().toString() == resources.getString(R.string.exponent)){
            operation.text = when(sign) {
                resources.getString(R.string.minus_btn) -> "${operation.text}$sign"
                else -> "${operation.text}$zero$sign"
            }
            return
        }
        if(resources.getStringArray(R.array.signs).contains(operation.text.last().toString()) ||
            operation.text.last().toString() == getPeriod(requireActivity())){
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
        val operation = requireView().findViewById<TextView>(R.id.currentOperationTextView)
        if(resultIsSelected() || operation.text.toString() == resources.getString(R.string.initial_operation)){ operation.text = "" }
        if(operation.text.isNullOrEmpty()){
            operation.text = "$function${resources.getString(R.string.open_bracket)}"
            return
        }
        if(operation.text.last().toString() == getPeriod(requireActivity())){return}
        if(operation.text.length + function.length >= resources.getInteger(R.integer.max_chars_in_line) - 1){return}
        if(resources.getStringArray(R.array.signs).contains(operation.text.last().toString()) ||
            operation.text.last().toString() == resources.getString(R.string.open_bracket)){
            operation.apply { text = "$text$function${resources.getString(R.string.open_bracket)}"}
            return
        }
        operation.apply { text = "$text${resources.getString(R.string.multiply_btn)}$function${resources.getString(R.string.open_bracket)}" }
    }

    @SuppressLint("SetTextI18n")
    private fun enterSquareRoot(){
        val operation = requireView().findViewById<TextView>(R.id.currentOperationTextView)
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
        if(operation.text.last().toString() == getPeriod(requireActivity()) || operation.text.last().toString() == root){return}
        if(resources.getStringArray(R.array.signs).contains(operation.text.last().toString()) ||
            operation.text.last().toString() == resources.getString(R.string.open_bracket)){
            operation.apply { text = "$text$root"}
            return
        }
        operation.apply { text = "$text${resources.getString(R.string.multiply_btn)}$root" }
    }

    @SuppressLint("SetTextI18n")
    private fun enterFactorial(){
        val operation = requireView().findViewById<TextView>(R.id.currentOperationTextView)
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
        val currantOperationTextView = requireView().findViewById<View>(R.id.currentOperationTextView) as TextView
        val currentText = currantOperationTextView.text
        val openBracket = resources.getString(R.string.open_bracket)
        val closeBracket = resources.getString(R.string.close_bracket)
        val multiply = resources.getString(R.string.multiply_btn)
        if(currentText.isEmpty() || currentText.toString() == resources.getString(R.string.initial_operation)){
            currantOperationTextView.text = openBracket
            return
        }
        if(currentText.last().toString() == getPeriod(requireActivity())){ return }
        currantOperationTextView.apply { text = when{
            resources.getStringArray(R.array.signs).contains(currentText.last().toString()) -> "$text$openBracket"
            currentText.last().toString() == openBracket || currentText.last().toString() == resources.getString(R.string.square_root_sign) -> "$text$openBracket"
            currentText.count { it.toString() == openBracket } <= currentText.count { it.toString() == closeBracket } -> when {
                isNumber(currentText.last()) -> "$text$multiply$openBracket"
                currentText.last().toString() == closeBracket -> "$text$multiply$openBracket"
                else -> "$text$openBracket"
            }
            else -> "$text${resources.getString(R.string.close_bracket)}"
        } }

    }

    @SuppressLint("SetTextI18n")
    private fun enterPeriod(){
        val currantOperationTextView = requireView().findViewById<View>(R.id.currentOperationTextView) as TextView
        val period = getPeriod(requireActivity()).first()
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
        if(signIndex < currentText.lastIndexOf(period) || (signIndex-2).coerceAtLeast(-1) < currentText.lastIndexOf(resources.getString(R.string.exponent))){return}
        currantOperationTextView.apply { text = "$text$period" }
    }
    @SuppressLint("SetTextI18n")
    private fun enterExponent(){
        val currantOperationTextView = requireView().findViewById<View>(R.id.currentOperationTextView) as TextView
        val exp = resources.getString(R.string.exponent)
        val currentText = currantOperationTextView.text
        if(currentText.isEmpty()){ return }
        if(currentText.toString() == resources.getString(R.string.initial_operation)){ return }
        if(!currentText.last().isDigit()){ return }
        var signIndex = -1
        resources.getStringArray(R.array.signs).forEach {sign ->
            signIndex = signIndex.coerceAtLeast(currentText.lastIndexOf(sign))
        }
        if(signIndex < currentText.lastIndexOf(exp)){return}
        currantOperationTextView.apply { text = "$text$exp" }
    }

    private fun inverseButtons(){
        val buttonSin = requireView().findViewById<Button>(R.id.button_sin)
        val buttonCos = requireView().findViewById<Button>(R.id.button_cos)
        val buttonTan = requireView().findViewById<Button>(R.id.button_tan)
        if(buttonSin.tag == resources.getString(R.string.sine_btn)){
            buttonSin.tag = resources.getString(R.string.arcsin)
            buttonSin.text = Html.fromHtml(resources.getString(R.string.arcsin_btn), Html.FROM_HTML_MODE_COMPACT)
        } else {
            buttonSin.tag = resources.getString(R.string.sine_btn)
            buttonSin.text = resources.getString(R.string.sine_btn)
        }
        if(buttonCos.tag == resources.getString(R.string.cosine_btn)){
            buttonCos.tag = resources.getString(R.string.arccos)
            buttonCos.text = Html.fromHtml(resources.getString(R.string.arccos_btn), Html.FROM_HTML_MODE_COMPACT)
        } else {
            buttonCos.tag = resources.getString(R.string.cosine_btn)
            buttonCos.text = resources.getString(R.string.cosine_btn)
        }
        if(buttonTan.tag == resources.getString(R.string.tan_btn)){
            buttonTan.tag = resources.getString(R.string.arctan)
            buttonTan.text = Html.fromHtml(resources.getString(R.string.arctan_btn), Html.FROM_HTML_MODE_COMPACT)
        } else {
            buttonTan.tag = resources.getString(R.string.tan_btn)
            buttonTan.text = resources.getString(R.string.tan_btn)
        }
    }

    private fun calculatePercents(){
        val operation = requireView().findViewById<TextView>(R.id.currentOperationTextView)
        var text = operation.text.toString()
        text = text.replace(getComma(requireActivity()), "")
        text = text.replace(getPeriod(requireActivity()), ".")
        val minus = resources.getString(R.string.minus_btn)
        val divide = resources.getString(R.string.divide_btn)
        val plus = resources.getString(R.string.plus_btn)
        val numberRegex = "(\\d+\\.?\\d*)([Ee]-?\\d+)?$".toRegex()
        val unchangeable = text.replace(numberRegex){""}
        text = text.replace(numberRegex) {
            if (unchangeable.isNotEmpty() && (unchangeable.last().toString() == plus || unchangeable.last().toString() == minus)) {
                validateResult((it.value.toDouble()/100*calculate(validatedOperation(unchangeable)).replace(getPeriod(requireActivity()), ".").toDouble()).toString(), requireActivity())
            } else {
                validateResult((it.value.toDouble()/100).toString(), requireActivity())
            }
        }
        operation.text = if(operation.text.toString() == text)
            calculate("${resources.getString(R.string.open_bracket)}${validatedOperation(text)}${resources.getString(R.string.close_bracket)}${divide}100")
        else text
        onTextChanged()

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
    private fun myCos(num: Double): Double{
        val inRadians = requireView().findViewById<Button>(R.id.units_btn).text.toString() == resources.getString(R.string.radians_units)
        return if(inRadians) cos(num) else cos(Math.toRadians(num))
    }

    private fun myTan(num: Double): Double{
        val inRadians = requireView().findViewById<Button>(R.id.units_btn).text.toString() == resources.getString(R.string.radians_units)
        return if(inRadians) tan(num) else tan(Math.toRadians(num))
    }

    private fun mySin(num: Double): Double{
        val inRadians = requireView().findViewById<Button>(R.id.units_btn).text.toString() == resources.getString(R.string.radians_units)
        return if(inRadians) sin(num) else sin(Math.toRadians(num))
    }

    private fun myArcSin(num: Double): Double{
        val inRadians = requireView().findViewById<Button>(R.id.units_btn).text.toString() == resources.getString(R.string.radians_units)
        return if(inRadians) asin(num) else Math.toDegrees(asin(num))
    }

    private fun myArcCos(num: Double): Double{
        val inRadians = requireView().findViewById<Button>(R.id.units_btn).text.toString() == resources.getString(R.string.radians_units)
        return if(inRadians) acos(num) else Math.toDegrees(acos(num))
    }

    private fun myArcTan(num: Double): Double{
        val inRadians = requireView().findViewById<Button>(R.id.units_btn).text.toString() == resources.getString(R.string.radians_units)
        return if(inRadians) atan(num) else Math.toDegrees(atan(num))
    }

    private fun calculateSimplified(text: String): String{
        val divide = resources.getString(R.string.divide_btn)
        val multiply = resources.getString(R.string.multiply_btn)
        val minus = resources.getString(R.string.minus_btn)
        val plus = resources.getString(R.string.plus_btn)
        val pow = resources.getString(R.string.raise_to_power_sign)
        val sin = resources.getString(R.string.sine_btn)
        val cos = resources.getString(R.string.cosine_btn)
        val tan = resources.getString(R.string.tan_btn)
        val asin = resources.getString(R.string.arcsin)
        val acos = resources.getString(R.string.arccos)
        val atan = resources.getString(R.string.arctan)
        val ln = resources.getString(R.string.log_natural_btn)
        val lg = resources.getString(R.string.log_10_btn)
        val sqrt = resources.getString(R.string.square_root_sign)
        val factorial = resources.getString(R.string.factorial_sign)
        val pi = resources.getString(R.string.pi_btn)
        val e = resources.getString(R.string.e_btn)
        val numberRegex = resources.getString(R.string.double_regex)
        var operation = text.replace(getComma(requireActivity()), "")
        operation = operation.replace(getPeriod(requireActivity()), ".")
        operation = operation.replace(pi, PI.toString())
        operation = operation.replace(e, Math.E.toString())
        operation = operation.replace("$atan$numberRegex".toRegex()){myArcTan(it.value.drop(atan.length).toDouble()).toString()}
        operation = operation.replace("$asin$numberRegex".toRegex()){
            val num = it.value.drop(asin.length).toDouble()
            if(abs(num) > 1) {throw ArithmeticException(resources.getString(R.string.asinError, validateResult(num.toString(), requireActivity())))}
            myArcSin(num).toString()
        }
        operation = operation.replace("$acos$numberRegex".toRegex()){
            val num = it.value.drop(asin.length).toDouble()
            if(abs(num) > 1) {throw ArithmeticException(resources.getString(R.string.acosError, validateResult(num.toString(), requireActivity())))}
            myArcCos(num).toString()
        }
        operation = operation.replace("$tan$numberRegex".toRegex()){
            val num = it.value.drop(tan.length).toDouble()
            if(myTan(num) > Int.MAX_VALUE) {throw ArithmeticException(resources.getString(R.string.tanError, validateResult(num.toString(), requireActivity())))}
            myTan(num).toString()
        }
        operation = operation.replace("$sin$numberRegex".toRegex()){mySin(it.value.drop(sin.length).toDouble()).toString()}
        operation = operation.replace("$cos$numberRegex".toRegex()){myCos(it.value.drop(cos.length).toDouble()).toString()}


        operation = operation.replace("$lg$numberRegex".toRegex()){
            if(it.value.drop(lg.length).toDouble() <= 0){throw ArithmeticException(resources.getString(R.string.logError))}
            log10(it.value.drop(lg.length).toDouble()).toString()
        }
        operation = operation.replace("$ln$numberRegex".toRegex()){
            if(it.value.drop(ln.length).toDouble() <= 0){throw ArithmeticException(resources.getString(R.string.logError))}
            ln(it.value.drop(ln.length).toDouble()).toString()
        }

        operation = operation.replace("--", "").replace("$sqrt$numberRegex".toRegex()){
            val res = it.value.drop(sqrt.length).toDouble().pow(0.5)
            if (res.isNaN()){throw ArithmeticException(resources.getString(R.string.sqrtError))}
            res.toString()
        }

        operation = operation.replace("$numberRegex$factorial".toRegex()){
            val num = it.value.dropLast(factorial.length).toDouble()
            val res = when{
                num >= 0 -> Gamma.gamma(num+1)
                else -> (-Gamma.gamma(-num+1))
            }
            if(abs(res) == Double.POSITIVE_INFINITY){throw ArithmeticException(resources.getString(R.string.tooLargeNumberError))}
            res.toString()
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
                var num2 = numberStack.removeLast().toDouble()
                var num1 = numberStack.removeLast().toDouble()
                if(signs[i].last().toString() == minus){num2 = -num2}
                if(signStack.last().last().toString() == minus){num1 = -num1}
                val res = if(signStack.last().length == 1) abs(num1.pow(num2)) else -num1.pow(num2)
                if (res.isNaN()){throw ArithmeticException(resources.getString(R.string.powError, validateResult(num1.toString(), requireActivity()), validateResult(num2.toString(), requireActivity())))}
                if(abs(res) == Double.POSITIVE_INFINITY){throw ArithmeticException(resources.getString(R.string.tooLargeNumberError))}
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
        return res.toString().replace(".", getPeriod(requireActivity()))
    }

    @SuppressLint("SetTextI18n")
    private fun clear(){
        (requireView().findViewById<View>(R.id.resultTextView) as TextView).text = "${resources.getString(R.string.equals_btn)}${resources.getString(R.string.delimiter)}${resources.getString(R.string.initial_operation)}"
        (requireView().findViewById<View>(R.id.currentOperationTextView) as TextView).text = resources.getString(R.string.initial_operation)
    }

    @SuppressLint("SetTextI18n")
    private fun clearAll(){
        (requireView().findViewById<View>(R.id.resultTextView) as TextView).text = "${resources.getString(R.string.equals_btn)}${resources.getString(R.string.delimiter)}${resources.getString(R.string.initial_operation)}"
        (requireView().findViewById<View>(R.id.currentOperationTextView) as TextView).text = resources.getString(R.string.initial_operation)
        (requireView().findViewById<View>(R.id.previousResultsTextView) as TextView).text = ""
    }


}