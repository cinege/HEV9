package com.example.hev_9

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

// JSON parse
import com.beust.klaxon.Klaxon

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
    var textViewFirst: TextView? = null
    var textViewSecond: TextView? = null
    var buttonFetch: Button? = null
    val viewModel: MainActivityViewModel by viewModels()

    private fun getRequest(sUrl: String): String? {
        val inputStream: InputStream
        var result: String? = null

        try {
            // Create URL
            val url = URL(sUrl)

            // Create HttpURLConnection
            val conn: HttpsURLConnection = url.openConnection() as HttpsURLConnection

            // Launch GET request
            conn.connect()

            // Receive response as inputStream
            inputStream = conn.inputStream

            if (inputStream != null)
            // Convert input stream to string
                result = inputStream.bufferedReader().use(BufferedReader::readText)
            else
                result = "error: inputStream is null"
        }
        catch(err:Error) {
            print("Error when executing get request: "+err.localizedMessage)
        }

        return result
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetch(sUrl: String) {
        var departures: Departures? = null
        //var blogInfo: BlogInfo? = null
        //var jo: JSONObject? = null
        lifecycleScope.launch(Dispatchers.IO) {
            val result = getRequest(sUrl)
            if (result != null) {
                try {
                    // Parse result string JSON to data class
                    departures = Departures(result)
                    withContext(Dispatchers.Main) {
                        // Update view model
                        viewModel.first.value = departures?.first
                        viewModel.second.value = departures?.second
                    }
                }
                catch(err:Error) {
                    print("Error when parsing JSON: "+err.localizedMessage)
                }
            }
            else {
                print("Error: Get request returned no response")
            }
        }
        //return blogInfo
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewFirst = findViewById(R.id.textview_first)
        textViewSecond = findViewById(R.id.textview_second)
        buttonFetch = findViewById(R.id.button_fetch)

        buttonFetch?.setOnClickListener(View.OnClickListener {
            // Launch get request
            fetch("https://futar.bkk.hu/api/query/v1/ws/otp/api/where/arrivals-and-departures-for-stop.json?stopId=BKK_004903&minutesBefore=1&minutesAfter=60")
        })

        viewModel.first.observe(this, Observer {
            textViewFirst?.text = it
        })

        viewModel.second.observe(this, Observer {
            textViewSecond?.text = it
        })
    }


}