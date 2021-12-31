package com.example.hev_9

import android.os.Build
import android.os.Bundle
import android.view.View
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
import androidx.lifecycle.Observer
import androidx.activity.viewModels
import androidx.annotation.RequiresApi

class MainActivity : AppCompatActivity() {
    val OrsID = "BKK_004903"
    val ArpadfoldID = "BKK_F03409"
    var textViewFirst: TextView? = null
    var textViewSecond: TextView? = null
    var buttonFetch: Button? = null
    var textViewThird: TextView? = null
    var textViewFourth: TextView? = null
    var buttonFetch2: Button? = null
    val viewModel: MainActivityViewModel by viewModels()

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

        buttonFetch?.setOnClickListener(View.OnClickListener {fetch(OrsID,"stopHeadsign", "Csömör")})
        buttonFetch2?.setOnClickListener(View.OnClickListener {fetch(ArpadfoldID, "stopHeadsign","Örs vezér tere")})

        viewModel.first.observe(this, Observer {textViewFirst?.text = it})
        viewModel.second.observe(this, Observer {textViewSecond?.text = it})
        viewModel.third.observe(this, Observer {textViewThird?.text = it})
        viewModel.fourth.observe(this, Observer {textViewFourth?.text = it})
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetch(stopID: String, attr: String, value: String) {
        var departures: Departures? = null
        lifecycleScope.launch(Dispatchers.IO) {
            val result = getRequest(stopID)
            print(result)
            if (result != null) {
                try {
                    departures = Departures(result, attr, value)
                    withContext(Dispatchers.Main) {
                        when(stopID){
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
                }
                catch(err:Error) {
                    print("Error when parsing JSON: "+err.localizedMessage)
                }
            } else {
                print("Error: Get request returned no response")
            }
        }
    }

    private fun getRequest(stopID: String): String? {
        val inputStream: InputStream
        var result: String? = null

        try {
            val sURL_part1 ="https://futar.bkk.hu/api/query/v1/ws/otp/api/where/arrivals-and-departures-for-stop.json?stopId="
            val sURL_part2 ="&minutesBefore=1&minutesAfter=60"
            val url = URL(sURL_part1 + stopID + sURL_part2)
            val conn: HttpsURLConnection = url.openConnection() as HttpsURLConnection
            conn.connect()
            inputStream = conn.inputStream
            if (inputStream != null)
                result = inputStream.bufferedReader().use(BufferedReader::readText)
            else
                result = "error: inputStream is null"
        }
        catch(err:Error) {
            print("Error when executing get request: "+err.localizedMessage)
        }
        return result
    }




}