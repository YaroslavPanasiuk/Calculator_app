package com.calculatorPackage.calculator

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import org.apache.commons.math3.util.FastMath.min
import java.util.Locale

class SharedMethods {


    companion object{

        public fun setTheme(context: Context) {
            val themeId = context.getSharedPreferences(
                context.resources.getString(R.string.settings_menuitem),
                AppCompatActivity.MODE_PRIVATE
            ).getInt(context.resources.getString(R.string.theme_setting), -1)
            when (themeId) {
                R.style.Theme_CalculatorOrange -> {
                    context.setTheme(if (themeIsDark(context)) R.style.Theme_CalculatorOrangeDark else R.style.Theme_CalculatorOrange)

                }
                R.style.Theme_CalculatorPurple -> { context.setTheme(R.style.Theme_CalculatorPurple) }
                R.style.Theme_CalculatorRed -> { context.setTheme(R.style.Theme_CalculatorRed) }
                R.style.Theme_CalculatorGreen -> {
                    context.setTheme(if (themeIsDark(context)) R.style.Theme_CalculatorGreenDark else R.style.Theme_CalculatorGreen)
                }
                R.style.Theme_CalculatorVanilla -> {
                    context.setTheme(if (themeIsDark(context)) R.style.Theme_CalculatorVanillaDark else R.style.Theme_CalculatorVanilla)
                }
                else -> {
                    context.setTheme(if (themeIsDark(context)) R.style.Theme_CalculatorDark else R.style.Theme_CalculatorLight)
                }
            }

        }
        @ColorInt
        public fun getColorFromAttr(theme: Resources.Theme, @AttrRes attrColor: Int, typedValue: TypedValue = TypedValue(), resolveRefs: Boolean = true): Int {
            theme.resolveAttribute(attrColor, typedValue, resolveRefs)
            return typedValue.data
        }
        private fun themeIsDark(context: Context) = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        public fun validateResult(result: String,context: Context, accuracy: Int = context.getSharedPreferences(context.resources.getString(R.string.settings_menuitem), Context.MODE_PRIVATE).getInt(context.resources.getString(R.string.accuracy_setting), context.resources.getInteger(R.integer.accuracy))):String{
            return if(result.replace(getPeriod(context), ".").toDoubleOrNull() != null)
                addNumberSeparator(roundHighOrders(result.replace(getPeriod(context), ".").toDouble().round(accuracy).toString().removeSuffix(".0").replace(".", getPeriod(context)), accuracy), context)
            else result
        }

        public fun roundHighOrders(text: String, accuracy: Int):String{
            return text.replace("(?<=[.,])\\d+(?=[eE])".toRegex()){it.value.subSequence(0, min(it.value.length, accuracy))}
        }

        public fun addNumberSeparator(text: String, context: Context):String{
            val none = context.resources.getString(R.string.separator_none)
            val universal = context.resources.getString(R.string.separator_universal)
            val indian = context.resources.getString(R.string.separator_indian)
            val settings = context.resources.getString(R.string.settings_menuitem)
            var separator = context.getSharedPreferences(settings, Context.MODE_PRIVATE).getString(context.resources.getString(R.string.separator_type_setting), none)!!
            if(context.getSharedPreferences(settings, Context.MODE_PRIVATE).getString(context.resources.getString(R.string.separator_setting), none) == none){ separator = none }
            return when(separator){
                universal -> text.replace(getComma(context), "").replace("(?<![${getPeriod(context)}\\d])(\\d*)".toRegex()){ match ->
                    buildString {
                        for(i in match.value.indices){
                            this.insert(0, match.value[match.value.length-1-i])
                            if(i%3 == 2){this.insert(0, getComma(context))}
                        }
                    }.removePrefix(getComma(context))
                }
                indian -> text.replace(getComma(context), "").replace("(?<![${getPeriod(context)}\\d])(\\d*)".toRegex()){ match ->
                    if(match.value.length <= 3) match.value
                    else buildString {
                        this.append(getComma(context))
                        this.append(match.value[match.value.length-3])
                        this.append(match.value[match.value.length-2])
                        this.append(match.value[match.value.length-1])
                        for(i in 3 until match.value.length){
                            this.insert(0, match.value[match.value.length-1-i])
                            if(i%2 == 0){this.insert(0, getComma(context))}
                        }
                    }.removePrefix(getComma(context))
                }
                else -> text
            }
        }

        private fun getAccuracy(context: Context) = context.resources.getInteger(R.integer.accuracy)

        private fun Double.round(accuracy: Int): Double {
            return roundHighOrders(String.format(Locale.ENGLISH, "%.${accuracy}f", this), accuracy).toDouble()
        }

        public fun getPeriod(context: Context) = context.getSharedPreferences(context.resources.getString(R.string.settings_menuitem), Context.MODE_PRIVATE).getString(context.resources.getString(R.string.period_setting), context.resources.getString(R.string.period_sign))!!
        public fun getComma(context: Context) = context.getSharedPreferences(context.resources.getString(R.string.settings_menuitem), Context.MODE_PRIVATE).getString(context.resources.getString(R.string.separator_setting), context.resources.getString(R.string.period_sign))!!

    }

}