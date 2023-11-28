package com.calculatorPackage.calculator

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.core.view.children
import androidx.fragment.app.Fragment

class ConverterFragment: Fragment(R.layout.fragment_converter) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<androidx.gridlayout.widget.GridLayout>(R.id.grid_converter)?.children?.forEach { btn ->
            btn.setOnClickListener {
                when((it as Button).text){
                    resources.getString(R.string.notation_btn) -> {
                        val intent = Intent(requireActivity(), NotationConverterActivity::class.java)
                        startActivity(intent)
                    }
                    else -> {
                        val intent = Intent(requireActivity(), ConverterActivity::class.java)
                        intent.putExtra(resources.getString(R.string.converterType), it.text)
                        startActivity(intent)
                    }
                }
            }
        }
    }
}