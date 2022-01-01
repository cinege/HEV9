package com.example.hev_9

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

// Imports for
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

// Networking
import java.io.BufferedReader
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

// View model
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import java.net.InetSocketAddress
import java.net.Socket
import android.net.NetworkInfo

import android.net.ConnectivityManager




class MainActivity : AppCompatActivity() {
    private val OrsID = "BKK_004903"
    private val ArpadfoldID = "BKK_F03409"
    private var textViewFirst: TextView? = null
    private var textViewSecond: TextView? = null
    private var buttonFetch: Button? = null
    private var textViewThird: TextView? = null
    private var textViewFourth: TextView? = null
    private var buttonFetch2: Button? = null
    private val viewModel: MainActivityViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewFirst = findViewById(R.id.textview_first)
        textViewSecond = findViewById(R.id.textview_second)
        buttonFetch = findViewById(R.id.button_fetch)
        textViewThird = findViewById(R.id.textview_third)
        textViewFourth = findViewById(R.id.textview_fourth)
        buttonFetch2 = findViewById(R.id.button_fetch2)

        buttonFetch?.setOnClickListener({fetch(OrsID,"stopHeadsign", "Csömör")})
        buttonFetch2?.setOnClickListener({fetch(ArpadfoldID, "stopHeadsign","Örs vezér tere")})

        viewModel.first.observe(this, {textViewFirst?.text = it})
        viewModel.second.observe(this, {textViewSecond?.text = it})
        viewModel.third.observe(this, {textViewThird?.text = it})
        viewModel.fourth.observe(this, {textViewFourth?.text = it})
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun fetch(stopID: String, attr: String, value: String) {
        if (true) {
            var departures: Departures?
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val result = getRequest(stopID)
                    departures = result?.let { Departures(it, attr, value) }
                    withContext(Dispatchers.Main) {
                        when (stopID) {
                            //Ors
                            "BKK_004903" -> {
                                viewModel.first.value = departures?.first
                                viewModel.second.value = departures?.second
                            }
                            //Arpadfold
                            "BKK_F03409" -> {
                                viewModel.third.value = departures?.first
                                viewModel.fourth.value = departures?.second
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Connection Error")
                }
            }
        }
    }

    private fun getRequest(stopID: String): String? {

        val inputStream: InputStream
        var result = "{}"

        val sURL_part1 ="https://futar.bkk.hu/api/query/v1/ws/otp/api/where/arrivals-and-departures-for-stop.json?stopId="
        val sURL_part2 ="&minutesBefore=1&minutesAfter=120"
        val url = URL(sURL_part1 + stopID + sURL_part2)
        val conn: HttpsURLConnection = url.openConnection() as HttpsURLConnection
        conn.connect()
        inputStream = conn.inputStream
        result = inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: "error: inputStream is null"
        return result
    }


}
