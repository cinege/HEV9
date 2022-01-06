package com.example.hev_9

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainActivityViewModel : ViewModel() {
    val status = MutableLiveData<String>()
    val arphev1 = MutableLiveData<String>()
    val arphev2 = MutableLiveData<String>()
    val arpbus1 = MutableLiveData<String>()
    val arpbus2 = MutableLiveData<String>()
    val orshev1 = MutableLiveData<String>()
    val orshev2 = MutableLiveData<String>()
    val orsbus1 = MutableLiveData<String>()
    val orsbus2 = MutableLiveData<String>()

}