package com.example.youtubewidgetkotlin

import android.content.Context
import android.content.ServiceConnection
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.sql.Connection

class VideosInformation (searchQuery: String, private val context: Context) {

    private val apiKeysArray =
        arrayOf(com.example.youtubewidgetkotlin.API_KEY.KEY,
            com.example.youtubewidgetkotlin.API_KEY.KEY1)
    private var currentAPIKEY = 0
    private var apiKey = apiKeysArray[0]
    private val videoInformationJSONObject: JSONObject?

    init {
        videoInformationJSONObject = getVideoInformationObject(searchQuery)
    }

    fun getFirstVideoInfo(): JSONObject? {
        return videoInformationJSONObject?.getJSONArray("items")?.
            getJSONObject(0)
    }

    private fun getVideoInformationObject(searchQuery: String): JSONObject? {
        var videoInformationObject: JSONObject? = null
        var availableSpareAPIKeys = true

        while (availableSpareAPIKeys) {

            val connection = createConnectionWithQuery(searchQuery)

            if (connection?.responseCode == HttpURLConnection.HTTP_OK) {
                videoInformationObject = createJSONObjectFromConnectionInputStream(connection)
                break
            } else if (connection?.responseCode == HttpURLConnection.HTTP_FORBIDDEN) { //FORBIDDEN = API Key limit reached
                availableSpareAPIKeys =  isSpareAPIKeyAvailable()
            }
        }
            return videoInformationObject
    }

    private fun createConnectionWithQuery(searchQuery: String): HttpURLConnection? {
        try {
            val connection: HttpURLConnection
            val url = URL(
                "https://www.googleapis.com/youtube/v3/search?part=snippet&q=" +
                        searchQuery + "&maxResults=1&type=video&key=" + apiKey
            )
            connection = url.openConnection() as HttpURLConnection
            connection.connect()
            return connection
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun isSpareAPIKeyAvailable(): Boolean {
        return if (isCurrentKeyTheLastAvailableKey()) {
            createToast("API limits exceeded, try again later")
            changeCurrentAPIKEYToFirst()
            false
        } else {
            changeCurrentAPIKEYToNext()
            true
        }
    }

    private fun createJSONObjectFromConnectionInputStream(connection: HttpURLConnection): JSONObject {
        val connectionInputStream = connection.inputStream
        val bufferedReader = BufferedReader(InputStreamReader(connectionInputStream))
        val result = StringBuilder()
        bufferedReader.forEachLine {
            result.append(it).append("\n")
        }
        return JSONObject(result.toString())
    }

    private fun isCurrentKeyTheLastAvailableKey(): Boolean {
        return currentAPIKEY == apiKeysArray.size - 1
    }

    private fun createToast(text: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }

    private fun changeCurrentAPIKEYToFirst() {
        currentAPIKEY = 0
        apiKey = apiKeysArray[currentAPIKEY]
    }

    private fun changeCurrentAPIKEYToNext() {
        apiKey = apiKeysArray[++currentAPIKEY]
        Log.d("AppInfo", "Changed key to $currentAPIKEY")
    }

}