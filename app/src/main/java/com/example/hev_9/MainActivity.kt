package com.example.hev_9

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
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId

class MainActivity : AppCompatActivity() {
    private val orsId = "BKK_004903"
    private val arpadfoldId = "BKK_F03409"
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

        buttonFetch?.setOnClickListener {fetch(orsId, "stopHeadsign", "Csömör") }
        buttonFetch2?.setOnClickListener{fetch(arpadfoldId, "stopHeadsign","Örs vezér tere")}

        viewModel.first.observe(this, {textViewFirst?.text = it})
        viewModel.second.observe(this, {textViewSecond?.text = it})
        viewModel.third.observe(this, {textViewThird?.text = it})
        viewModel.fourth.observe(this, {textViewFourth?.text = it})
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun fetch(stopID: String, attr: String, value: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = getRequest(stopID)
                val departures = processJSON(result, attr, value)
                withContext(Dispatchers.Main) {uiUpdate(stopID, departures)}
            } catch (e: Exception) {
            }
        }
    }

    private fun uiUpdate(stopID: String, departures: Departures){
        when (stopID) {
            //Ors
            "BKK_004903" -> {
                viewModel.first.value = departures.first
                viewModel.second.value = departures.second
            }
            //Arpadfold
            "BKK_F03409" -> {
                viewModel.third.value = departures.first
                viewModel.fourth.value = departures.second
            }
        }
    }

    private fun getRequest(stopID: String): String {
        var result: String
        try {
            val inputStream: InputStream
            val sURLpart1 = "https://futar.bkk.hu/api/query/v1/ws/otp/api/where/arrivals-and-departures-for-stop.json?stopId="
            val sURLpart2 = "&minutesBefore=1&minutesAfter=120"
            val url = URL(sURLpart1 + stopID + sURLpart2)
            val conn: HttpsURLConnection = url.openConnection() as HttpsURLConnection
            conn.connect()
            inputStream = conn.inputStream
            result = inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: "Nincs adat"
        } catch (err:Error){
            result = "Hálózati hiba"
        }
        return result
    }

    private fun processJSON (jsonstring: String, attr: String, value: String) : Departures {
        val result = Departures("", "")
            try {
                if (jsonstring.length > 20) {
                    val jobj = JSONObject(jsonstring)
                    val results = ArrayList<String>()
                    val departures =
                        jobj.getJSONObject("data").getJSONObject("entry").getJSONArray("stopTimes")

                    for (i in 0 until departures.length()) {
                        val departure = departures.getJSONObject(i)
                        if (departure.getString(attr).equals(value)) {
                            results.add(convEpochtoString(departure.getInt("departureTime")))
                        }
                    }
                    result.first = if (results.size > 0) results[0] else "-"
                    result.second = if (results.size > 1) results[1] else "-"
                } else {
                    result.first = jsonstring
                    result.second = jsonstring
                }
            } catch (err:Error) {
                result.first = "Nem dolgozható fel"
                result.second = "Nem dolgozható fel"
            }
        return result
    }

    private fun convEpochtoString(time: Int): String {
        val now = LocalDateTime.now()
        //Departures (time) represent UTC time epoch values in seconds
        val offset = ZoneId.systemDefault().rules.getOffset(now).totalSeconds
        //Winter: UTC + 1h Summer: UTC + 2h
        val timeinMin = (time + offset) % (24 * 3600) / 60
        val hour = timeinMin / 60
        val min = timeinMin % 60
        val hpad = if (hour < 10) "0" else ""
        val mpad = if (min < 10) "0" else ""
        //return hh:mm format
        return "$hpad$hour:$mpad$min"
    }
}
