package com.example.hev_9

import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.collections.ArrayList

class Departures(jsonstring: String, attr: String, value: String) {
    var first: String
    var second: String

    init {
        try {
            val jobj = JSONObject(jsonstring)
            val result = ArrayList<String>()
            val departures =
                jobj.getJSONObject("data").getJSONObject("entry").getJSONArray("stopTimes")

            for (i in 0 until departures.length()) {
                val departure = departures.getJSONObject(i)
                if (departure.getString(attr).equals(value)) {
                    result.add(convEpochtoString(departure.getInt("departureTime")))
                }
            }
            this.first = if (result.size > 0) result[0] else "-"
            this.second = if (result.size > 1) result[1] else "-"
        } catch (err:Error) {
            this.first = "-"
            this.second = "-"
        }
    }

    private fun convEpochtoString(time: Int): String {
        val now = LocalDateTime.now()
        val offset = ZoneId.systemDefault().rules.getOffset(now).totalSeconds
        val timeinMin = (time + offset) % (24 * 3600) / 60
        val hour = timeinMin / 60
        val min = timeinMin % 60
        val hpad = if (hour < 10) "0" else ""
        val mpad = if (min < 10) "0" else ""
        return "$hpad$hour:$mpad$min"
    }
}