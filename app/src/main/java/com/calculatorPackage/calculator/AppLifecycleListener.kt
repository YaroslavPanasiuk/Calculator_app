package com.calculatorPackage.calculator

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class AppLifecycleListener() : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) { // app moved to foreground
    }

    override fun onStop(owner: LifecycleOwner) { // app moved to background
    }
}