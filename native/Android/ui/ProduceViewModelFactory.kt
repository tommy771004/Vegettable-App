package com.example.produceapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.produceapp.network.ProduceService

class ProduceViewModelFactory(private val service: ProduceService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProduceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProduceViewModel(service) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
