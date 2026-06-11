package com.mtphub.updater

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UpdateProgress {
    private val _progress = MutableStateFlow(0)  // 0..100
    val progress: StateFlow<Int> = _progress.asStateFlow()

    fun setProgress(value: Int) {
        _progress.value = value
    }

    fun reset() {
        _progress.value = 0
    }
}