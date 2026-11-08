package com.example.myfyp

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.example.myfyp.ui.theme.MyFYPTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PREFERENCES_NAME = "iepms_preferences"
private const val KEY_LAST_IP = "last_ip_address"
private const val KEY_ENGLISH = "english_language"

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
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE) }
    val savedIp = remember { preferences.getString(KEY_LAST_IP, "").orEmpty() }
    var ipAddress by remember { mutableStateOf(savedIp) }
    var activeHost by remember { mutableStateOf(savedIp) }
    var english by remember { mutableStateOf(preferences.getBoolean(KEY_ENGLISH, false)) }
    var readings by remember { mutableStateOf(SensorReadings()) }
    var history by remember { mutableStateOf(listOf<HistoryPoint>()) }
    var status by remember { mutableStateOf(if (savedIp.isBlank()) "未连接" else "正在自动连接…") }
    var lastUpdated by remember { mutableStateOf("--") }
    var refreshToken by remember { mutableStateOf(0) }

    val text = if (english) EnglishText else ChineseText

    suspend fun refresh() {
        val host = activeHost.trim().removePrefix("http://").removeSuffix("/")
        if (host.isEmpty()) {
            status = text.enterIp
            return
        }
        status = text.connecting
        try {
            val result = withContext(Dispatchers.IO) { fetchReadings(host) }
            readings = result
            val now = System.currentTimeMillis()
            history = (history + HistoryPoint(now, result)).filter { it.timestamp >= now - 60_000L }
            status = text.connected
            lastUpdated = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        } catch (error: Exception) {
            status = "${text.connectionFailed}: ${error.message ?: text.httpFailed}"
        }
    }

    LaunchedEffect(activeHost, refreshToken, english) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (english) "EN" else "中")
                Switch(
                    checked = english,
                    onCheckedChange = { selected ->
                        english = selected
                        preferences.edit().putBoolean(KEY_ENGLISH, selected).apply()
                    }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = ipAddress,
                onValueChange = { value ->
                    ipAddress = value
                    preferences.edit().putString(KEY_LAST_IP, value).apply()
                },
                label = { Text(text.ipLabel) },
                placeholder = { Text(text.ipPlaceholder) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                activeHost = ipAddress
                refreshToken++
            }) { Text(text.connect) }
        }

        Text("${text.statusLabel}: $status")
        Text("${text.updatedLabel}: $lastUpdated")
        Button(onClick = { refreshToken++ }, modifier = Modifier.fillMaxWidth()) { Text(text.refresh) }
        SensorCard(text.temperature, readings.temperature?.let { "%.2f °C".format(Locale.US, it) } ?: "--")
        SensorCard(text.humidity, readings.humidity?.let { "%.2f %%".format(Locale.US, it) } ?: "--")
        SensorCard(text.illuminance, readings.illuminance?.let { "%.2f lux".format(Locale.US, it) } ?: "--")
        SensorCard(text.pm1, readings.pm1?.let { "$it µg/m³" } ?: "--")
        SensorCard(text.pm25, readings.pm25?.let { "$it µg/m³" } ?: "--")
        SensorCard(text.pm10, readings.pm10?.let { "$it µg/m³" } ?: "--")
        MeasurementChart(text.temperature, history, { it.temperature }, Color(0xFFE53935))
        MeasurementChart(text.humidity, history, { it.humidity }, Color(0xFF1E88E5))
        MeasurementChart(text.illuminance, history, { it.illuminance }, Color(0xFFF9A825))
        MeasurementChart(text.pm1, history, { it.pm1?.toDouble() }, Color(0xFF43A047))
        MeasurementChart(text.pm25, history, { it.pm25?.toDouble() }, Color(0xFF8E24AA))
        MeasurementChart(text.pm10, history, { it.pm10?.toDouble() }, Color(0xFF00897B))
    }
}

@Composable
private fun SensorCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun MeasurementChart(
    label: String,
    history: List<HistoryPoint>,
    valueSelector: (SensorReadings) -> Double?,
    lineColor: Color,
) {
    val now = System.currentTimeMillis()
    val values = history.mapNotNull { point -> valueSelector(point.readings)?.let { point.timestamp to it } }
    val minValue = values.minOfOrNull { it.second } ?: 0.0
    val maxValue = values.maxOfOrNull { it.second } ?: 1.0
    val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
    val chartMin = minValue - range * 0.1
    val chartMax = maxValue + range * 0.1

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                Column(
                    modifier = Modifier.fillMaxHeight().padding(end = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatChartValue(chartMax), style = MaterialTheme.typography.labelSmall)
                    Text(formatChartValue(chartMin), style = MaterialTheme.typography.labelSmall)
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val left = 8f
                        val right = size.width - 4f
                        val top = 4f
                        val bottom = size.height - 8f
                        drawLine(Color.LightGray, Offset(left, top), Offset(left, bottom), 1.5f)
                        drawLine(Color.LightGray, Offset(left, bottom), Offset(right, bottom), 1.5f)
                        drawLine(Color(0xFFE0E0E0), Offset(left, top + (bottom - top) / 2), Offset(right, top + (bottom - top) / 2), 1f)

                        val path = Path()
                        var hasPoint = false
                        values.forEach { (timestamp, value) ->
                            val x = left + ((timestamp - (now - 60_000L)).toFloat() / 60_000f).coerceIn(0f, 1f) * (right - left)
                            val y = bottom - ((value - chartMin) / (chartMax - chartMin)).toFloat().coerceIn(0f, 1f) * (bottom - top)
                            if (!hasPoint) {
                                path.moveTo(x, y)
                                hasPoint = true
                            } else {
                                path.lineTo(x, y)
                            }
                        }
                        if (hasPoint) {
                            drawPath(path, lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
                        }
                    }
                    if (values.isEmpty()) {
                        Text("--", modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("-60s", style = MaterialTheme.typography.labelSmall)
                Text("now", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun formatChartValue(value: Double): String =
    if (value >= 100.0 || value == value.toInt().toDouble()) "%.0f".format(Locale.US, value)
    else "%.1f".format(Locale.US, value)

private data class UiText(
    val title: String,
    val ipLabel: String,
    val ipPlaceholder: String,
    val connect: String,
    val refresh: String,
    val statusLabel: String,
    val updatedLabel: String,
    val temperature: String,
    val humidity: String,
    val illuminance: String,
    val pm1: String,
    val pm25: String,
    val pm10: String,
    val enterIp: String,
    val connecting: String,
    val connected: String,
    val connectionFailed: String,
    val httpFailed: String,
)

private val ChineseText = UiText(
    title = "室内环境参数监测系统",
    ipLabel = "ESP32 IP 地址",
    ipPlaceholder = "例如 192.168.1.100",
    connect = "连接",
    refresh = "手动刷新",
    statusLabel = "Wi‑Fi/HTTP 状态",
    updatedLabel = "最后更新时间",
    temperature = "温度",
    humidity = "湿度",
    illuminance = "光照强度",
    pm1 = "PM1.0",
    pm25 = "PM2.5",
    pm10 = "PM10",
    enterIp = "请输入 ESP32 IP 地址",
    connecting = "正在连接…",
    connected = "已连接",
    connectionFailed = "连接失败",
    httpFailed = "HTTP 请求失败",
)

private val EnglishText = UiText(
    title = "Indoor Environmental Parameter Monitoring System",
    ipLabel = "ESP32 IP address",
    ipPlaceholder = "For example, 192.168.1.100",
    connect = "Connect",
    refresh = "Refresh",
    statusLabel = "Wi‑Fi/HTTP status",
    updatedLabel = "Last updated",
    temperature = "Temperature",
    humidity = "Humidity",
    illuminance = "Illuminance",
    pm1 = "PM1.0",
    pm25 = "PM2.5",
    pm10 = "PM10",
    enterIp = "Enter the ESP32 IP address",
    connecting = "Connecting…",
    connected = "Connected",
    connectionFailed = "Connection failed",
    httpFailed = "HTTP request failed",
)

private data class SensorReadings(
    val temperature: Double? = null,
    val humidity: Double? = null,
    val illuminance: Double? = null,
    val pm1: Int? = null,
    val pm25: Int? = null,
    val pm10: Int? = null,
)

private data class HistoryPoint(val timestamp: Long, val readings: SensorReadings)

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

private fun JSONObject.optNullableDouble(name: String): Double? =
    if (isNull(name)) null else optDouble(name).takeUnless { it.isNaN() }

private fun JSONObject.optNullableInt(name: String): Int? =
    if (isNull(name)) null else optInt(name)
