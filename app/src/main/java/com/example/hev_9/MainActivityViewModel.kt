package com.example.hev_9

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainActivityViewModel : ViewModel() {
    val first = MutableLiveData<String>()
    val second = MutableLiveData<String>()
    val third = MutableLiveData<String>()
    val fourth = MutableLiveData<String>()
}