package com.xianfeng.wjcscs

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class FileViewModelFactory(
    private val application: Application,
    private val networkService: NetworkService
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FileViewModel(application, networkService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}