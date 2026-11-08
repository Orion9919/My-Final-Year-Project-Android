package com.example.myfyp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.example.myfyp.ui.theme.MyFYPTheme
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyFYPTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        SensorScreen(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
@Composable
private fun SensorScreen(modifier: Modifier = Modifier) {
    var ipAddress by remember { mutableStateOf("") }
    var activeHost by remember { mutableStateOf("") }
    var readings by remember { mutableStateOf(SensorReadings()) }
    var status by remember { mutableStateOf("未连接") }
    var lastUpdated by remember { mutableStateOf("--") }
    var refreshToken by remember { mutableStateOf(0) }

    suspend fun refresh() {
        val host = activeHost.trim().removePrefix("http://").removeSuffix("/")
        if (host.isEmpty()) {
            status = "请输入 ESP32 IP 地址"
            return
        }
        status = "正在连接…"
        try {
            val result = withContext(Dispatchers.IO) { fetchReadings(host) }
            readings = result
            status = "已连接"
            lastUpdated = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        } catch (error: Exception) {
            status = "连接失败：${error.message ?: "HTTP 请求失败"}"
        }
    }

    LaunchedEffect(activeHost, refreshToken) {
        if (activeHost.isNotBlank()) {
            while (true) {
                refresh()
                delay(2000)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("FYP 环境监测", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = ipAddress,
                onValueChange = { ipAddress = it },
                label = { Text("ESP32 IP 地址") },
                placeholder = { Text("例如 192.168.1.100") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                activeHost = ipAddress
                refreshToken++
            }) { Text("连接") }
        }
        Text("Wi‑Fi/HTTP 状态：$status")
        Text("最后更新时间：$lastUpdated")
        Button(onClick = { refreshToken++ }, modifier = Modifier.fillMaxWidth()) { Text("手动刷新") }
        SensorCard("温度", readings.temperature?.let { "%.2f °C".format(Locale.US, it) } ?: "--")
        SensorCard("湿度", readings.humidity?.let { "%.2f %%".format(Locale.US, it) } ?: "--")
        SensorCard("光照强度", readings.illuminance?.let { "%.2f lux".format(Locale.US, it) } ?: "--")
        SensorCard("PM1.0", readings.pm1?.let { "$it µg/m³" } ?: "--")
        SensorCard("PM2.5", readings.pm25?.let { "$it µg/m³" } ?: "--")
        SensorCard("PM10", readings.pm10?.let { "$it µg/m³" } ?: "--")
    }
}

@Composable
private fun SensorCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private data class SensorReadings(
    val temperature: Double? = null,
    val humidity: Double? = null,
    val illuminance: Double? = null,
    val pm1: Int? = null,
    val pm25: Int? = null,
    val pm10: Int? = null,
)

private fun fetchReadings(host: String): SensorReadings {
    val connection = (URL("http://$host/api/sensors").openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 3000
        readTimeout = 3000
        setRequestProperty("Accept", "application/json")
    }
    return try {
        if (connection.responseCode !in 200..299) throw Exception("HTTP ${connection.responseCode}")
        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        SensorReadings(
            temperature = json.optNullableDouble("temperature"),
            humidity = json.optNullableDouble("humidity"),
            illuminance = json.optNullableDouble("illuminance"),
            pm1 = json.optNullableInt("pm1_0"),
            pm25 = json.optNullableInt("pm2_5"),
            pm10 = json.optNullableInt("pm10"),
        )
    } finally {
        connection.disconnect()
    }
}

private fun JSONObject.optNullableDouble(name: String): Double? = if (isNull(name)) null else optDouble(name).takeUnless { it.isNaN() }
private fun JSONObject.optNullableInt(name: String): Int? = if (isNull(name)) null else optInt(name)
