package com.example.shufflerion

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.shufflerion.ui.theme.ShufflerionTheme
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : ComponentActivity() {
    private val clientId = "335ea7b32dd24009bd0529ba85f0f8cc"
    private val redirectUri = "shufflerionApp://callback"
    private var requestCode = 1337
    private var accessToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        getAccessToken()

        setContent {
            ShufflerionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        Greeting(name = "Android")
                        PlayRandomSongButton()
                    }
                }
            }
        }
    }

    private fun getAccessToken() {
        val authBuilder = AuthorizationRequest.Builder(clientId, AuthorizationResponse.Type.TOKEN, redirectUri)
        authBuilder.setScopes(arrayOf("streaming", "user-modify-playback-state", "user-read-playback-state"))
        val request = authBuilder.build()
        AuthorizationClient.openLoginActivity(this, requestCode, request)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == this.requestCode) {
            val response = AuthorizationClient.getResponse(resultCode, data)

            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    accessToken = response.accessToken
                    Log.d("MainActivity", "Access Token: $accessToken")
                }
                AuthorizationResponse.Type.ERROR -> {
                    Log.e("MainActivity", "Error: ${response.error}")
                }
                else -> {
                    Log.d("MainActivity", "Respuesta no manejada: ${response.type}")
                }
            }
        }
    }

    private fun getDeviceIdAndPlayRandomSong() {
        if (accessToken == null) {
            Log.e("MainActivity", "El token de acceso no está disponible.")
            return
        }

        // Paso 1: Obtener el deviceId
        val url = "https://api.spotify.com/v1/me/player/devices"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "Error al obtener dispositivos: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Parsear la respuesta JSON para obtener el deviceId
                    val jsonResponse = JSONObject(response.body?.string() ?: "")
                    val devices = jsonResponse.getJSONArray("devices")

                    if (devices.length() > 0) {
                        // Obtener el primer dispositivo
                        val deviceId = devices.getJSONObject(0).getString("id")
                        val deviceName = devices.getJSONObject(0).getString("name")
                        Log.d("MainActivity", "Device ID: $deviceId")
                        Log.d("MainActivity", "Device Name: $deviceName")


                        // Paso 2: Reproducir una canción en el dispositivo
                        playRandomSongOnDevice(deviceId)
                    } else {
                        Log.e("MainActivity", "No se encontraron dispositivos disponibles.")
                    }
                } else {
                    Log.e("MainActivity", "Error al obtener los dispositivos: ${response.message}")
                }
            }
        })
    }

    private fun playRandomSongOnDevice(deviceId: String) {
        val url = "https://api.spotify.com/v1/me/player/play?device_id=$deviceId"
        val body = JSONObject().apply {
            val urisArray = JSONArray()
            urisArray.put("spotify:track:2s99JIa7LENyy9vmtBCrwR")
            put("uris", urisArray)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .put(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "Error al reproducir la canción: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("MainActivity", "Canción reproducida con éxito!")
                } else {
                    Log.e("MainActivity", "Error en la respuesta: ${response}")
                }
            }
        })
    }

    @Composable
    fun PlayRandomSongButton() {
        Button(onClick = { getDeviceIdAndPlayRandomSong() }) {
            Text(text = "Reproducir Canción Aleatoria")
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ShufflerionTheme {
        Greeting("Android")
    }
}
