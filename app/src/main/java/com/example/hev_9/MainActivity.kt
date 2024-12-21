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
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.UnknownHostException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private var textViewStatus: TextView? = null
    private var textViewArpHev1: TextView? = null
    private var textViewArpHev2: TextView? = null
    private var textViewArpBus1: TextView? = null
    private var textViewArpBus2: TextView? = null
    private var buttonFetchArp: Button? = null
    private var textViewOrsHev1: TextView? = null
    private var textViewOrsHev2: TextView? = null
    private var textViewOrsBus1: TextView? = null
    private var textViewOrsBus2: TextView? = null
    private var buttonFetchOrs: Button? = null
    private val viewModel: MainActivityViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewStatus = findViewById(R.id.textview_status)
        textViewArpHev1 = findViewById(R.id.textview_arphev1)
        textViewArpHev2 = findViewById(R.id.textview_arphev2)
        textViewArpBus1 = findViewById(R.id.textview_arpbus1)
        textViewArpBus2 = findViewById(R.id.textview_arpbus2)
        buttonFetchArp = findViewById(R.id.button_fetcharp)

        textViewOrsHev1 = findViewById(R.id.textview_orshev1)
        textViewOrsHev2 = findViewById(R.id.textview_orshev2)
        textViewOrsBus1 = findViewById(R.id.textview_orsbus1)
        textViewOrsBus2 = findViewById(R.id.textview_orsbus2)
        buttonFetchOrs = findViewById(R.id.button_fetchors)

        buttonFetchArp?.setOnClickListener{collect(0)}
        buttonFetchOrs?.setOnClickListener{collect(1)}

        viewModel.status.observe(this, {textViewStatus?.text = it})
        viewModel.arphev1.observe(this, {textViewArpHev1?.text = it})
        viewModel.arphev2.observe(this, {textViewArpHev2?.text = it})
        viewModel.arpbus1.observe(this, {textViewArpBus1?.text = it})
        viewModel.arpbus2.observe(this, {textViewArpBus2?.text = it})
        viewModel.orshev1.observe(this, {textViewOrsHev1?.text = it})
        viewModel.orshev2.observe(this, {textViewOrsHev2?.text = it})
        viewModel.orsbus1.observe(this, {textViewOrsBus1?.text = it})
        viewModel.orsbus2.observe(this, {textViewOrsBus2?.text = it})
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun collect(loc: Int) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val result = this@MainActivity.getRequest(loc)
                val departures = processJSONs(result, loc)
                withContext(Dispatchers.Main) {uiUpdate(loc, departures)}
            } catch (e: UnknownHostException) {
                withContext(Dispatchers.Main) {uiUpdate(loc, Departures("Nincs kapcsolat","-", "-", "-", "-"))}
            } catch (e: JSONException) {
                withContext(Dispatchers.Main) {uiUpdate(loc, Departures("Váratlan adatformátum","-", "-", "-", "-"))}
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {uiUpdate(loc, Departures("Hiba","-", "-", "-", "-"))}
            }
        }
    }

    @Throws(UnknownHostException::class)
    private fun getRequest(loc: Int): Array<String> {
        // Arpadfold H9 BKK_19830298 B31 BKK_F03299 Ors H9 BKK_19795278 B31 BKK_F02755
        val stations = arrayOf(arrayOf("BKK_19830298","BKK_F03299"), arrayOf("BKK_19795278","BKK_F02755"))
        val result = arrayOf("","")
        var inputStream: InputStream
        val sURLpart1 = "https://futar.bkk.hu/api/query/v1/ws/otp/api/where/arrivals-and-departures-for-stop.json?stopId="
        val sURLpart2 = "&minutesBefore=1&minutesAfter=120"
        // api key
        val sURLpart3 = "&key=18755a33-d3cd-4a32-a0e4-68b790f71e95"
        for (i in 0..1) {
            val url = URL(sURLpart1 + stations[loc][i] + sURLpart2 + sURLpart3)
            val conn: HttpsURLConnection = url.openConnection() as HttpsURLConnection
            conn.connect()
            inputStream = conn.inputStream
            result[i] = inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: "Nincs adat"
        }
        return result
    }

    @Throws(JSONException::class)
    private fun processJSONs (strings: Array<String>, loc: Int) : Departures {
        val result = arrayOf("","","","")
        val direction = arrayOf("tobp", "frombp")
        val hevDepartures = filterHEV(getDepartures(strings[0]), direction[loc])
        val busDepartures = getDepartures(strings[1])
        val status = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd         HH:mm:ss"))
        for (i in 0..1) {
            if (hevDepartures.length() > i) {
                result[i] = convEpochtoString(hevDepartures.getJSONObject(i).getInt("departureTime"))
                if (hevDepartures.getJSONObject(i).getString("stopHeadsign").equals("Cinkota")) {
                    result[i] = result[i] + "C"
                }
            } else {
                result[i] = "-"
            }
            if (busDepartures.length() > i) {
                result[2 + i] = processBusEntry(busDepartures.getJSONObject(i))
            } else {
                result[2 + i] = "-"
            }
        }
        return Departures(status, result[0], result[1], result[2], result[3])
    }

    @Throws(JSONException::class)
    fun filterHEV(departures: JSONArray, direction: String): JSONArray {
        val result = ArrayList<JSONObject>()
        for (i in 0 until departures.length()) {
            val departure = departures.getJSONObject(i)
            val stopheadsign = departures.getJSONObject(i).getString("stopHeadsign")
            if (direction.equals("tobp") && (stopheadsign.equals("Örs vezér tere") || stopheadsign.equals("Cinkota")) ||
                direction.equals("frombp") && (stopheadsign.equals("Csömör"))) {
                result.add(departure)
            }
        }
        return JSONArray(result)
    }
    private fun processBusEntry(busentry: JSONObject): String {
           return convEpochtoString(busentry.getInt("departureTime"))
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

    private fun getDepartures(json: String) : JSONArray  {
        return JSONObject(json)
            .getJSONObject("data")
            .getJSONObject("entry")
            .getJSONArray("stopTimes")
    }

    private fun uiUpdate(loc: Int, departures: Departures){
        viewModel.status.value = departures.status
        when (loc) {
            //Arpadfold
            0 -> {
                viewModel.arphev1.value = departures.hev1
                viewModel.arphev2.value = departures.hev2
                viewModel.arpbus1.value = departures.bus1
                viewModel.arpbus2.value = departures.bus2
            }
            //Ors
            1 -> {
                viewModel.orshev1.value = departures.hev1
                viewModel.orshev2.value = departures.hev2
                viewModel.orsbus1.value = departures.bus1
                viewModel.orsbus2.value = departures.bus2
            }

        }
    }
}


