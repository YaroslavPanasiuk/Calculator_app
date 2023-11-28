package com.calculatorPackage.calculator

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MyAdapter(activity: AppCompatActivity): FragmentStateAdapter(activity) {
    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> CalculatorFragment()
            else -> ConverterFragment()
        }
    }

    override fun getItemCount(): Int {
        return 2
    }
}