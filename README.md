# FYP Android Sensor Monitor

## 中文说明

这是 FYP 项目的 Android 测试应用 IEPMS（室内环境参数监测系统），使用 Kotlin 和 Jetpack Compose 开发。应用通过局域网 HTTP 请求读取 ESP32 的实时传感器数据。

支持显示：

- 温度（°C）
- 湿度（%）
- 光照强度（lux）
- PM1.0、PM2.5、PM10（µg/m³）

应用界面右上角提供中英文切换开关，并提供 ESP32 IP 地址输入、连接、手动刷新、Wi‑Fi/HTTP 状态和最后更新时间。IP 地址会在退出后保留；重新打开应用时会自动尝试连接上次地址。修改 IP 输入框后不会自动切换连接目标，必须点击“连接”按钮。连接成功后，应用约每 2 秒请求一次，并在数字结果下方显示六个按上下排列的测量曲线，每条曲线保留最近 60 秒的数据。

```text
GET http://<ESP32_IP>/api/sensors
```

ESP32 接口返回的 JSON 字段为 `temperature`、`humidity`、`illuminance`、`pm1_0`、`pm2_5` 和 `pm10`。无效的传感器数据会以 `null` 返回并在界面显示为 `--`。

### 使用 APK

1. 先按照 ESP32 固件项目说明配置 Wi‑Fi 并烧录固件。
2. 从 GitHub Releases 下载 `FYP-Android-debug.apk`。
3. 在 Android 手机上允许安装该 APK。
4. 让手机和 ESP32 连接同一个局域网。
5. 查看 ESP32 串口日志中的 IP 地址，在 App 中输入并点击“连接”。

### 从源码构建

在本目录执行：

```powershell
gradlew.bat :app:assembleDebug
```

APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## English

This is IEPMS (Indoor Environmental Parameter Monitoring System), the Android test application for the FYP project. It is built with Kotlin and Jetpack Compose and reads live sensor data from the ESP32 over a local HTTP connection.

The app displays:

- Temperature (°C)
- Humidity (%)
- Illuminance (lux)
- PM1.0, PM2.5 and PM10 (µg/m³)

The top-right language switch changes between Chinese and English. The UI provides an ESP32 IP address field, connect and manual refresh buttons, Wi‑Fi/HTTP status, and the last update time. The last IP address is preserved after the app exits and is automatically connected when the app starts again. Editing the IP address does not change the active connection until the user taps **连接 / Connect**. After connecting, the app polls the ESP32 approximately every two seconds and displays six vertically stacked measurement charts below the numeric results, keeping the latest 60 seconds of data.

```text
GET http://<ESP32_IP>/api/sensors
```

The endpoint returns the JSON fields `temperature`, `humidity`, `illuminance`, `pm1_0`, `pm2_5` and `pm10`. Invalid sensor readings are returned as `null` and shown as `--` in the app.

### Using the APK

1. Configure Wi‑Fi and flash the ESP32 firmware according to the firmware project instructions.
2. Download `FYP-Android-debug.apk` from GitHub Releases.
3. Allow installation of the APK on the Android phone.
4. Connect the phone and ESP32 to the same local network.
5. Find the ESP32 IP address in the serial monitor, enter it in the app, and tap **连接 / Connect**.

### Building from source

Run this command from the project directory:

```powershell
gradlew.bat :app:assembleDebug
```

The APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Changelog / 变更记录

### v1.0.2

中文：

- 在六项数字结果下方新增六个纵向排列的测量曲线。
- 横轴为最近 60 秒时间，纵轴为对应测量结果。
- 温度、湿度、光照、PM1.0、PM2.5、PM10 使用不同颜色显示。
- 曲线只保存在内存中，不增加数据库或历史文件。

English:

- Added six vertically stacked measurement charts below the numeric results.
- The horizontal axis covers the latest 60 seconds and the vertical axis shows the corresponding measurement value.
- Temperature, humidity, illuminance, PM1.0, PM2.5 and PM10 use different line colors.
- Chart history is kept in memory only; no database or history file is added.

### v1.0.1

中文：

- 新增右上角中英文语言切换开关。
- 应用名称改为 `IEPMS`。
- 中英文标题分别显示为“室内环境参数监测系统”和“Indoor Environmental Parameter Monitoring System”。
- 保存上次输入的 ESP32 IP 地址，重新打开应用后自动连接。
- 修改 IP 后必须点击“连接”按钮才会切换轮询目标。

English:

- Added a Chinese/English language switch in the top-right corner.
- Renamed the application to `IEPMS`.
- Added localized titles: “室内环境参数监测系统” and “Indoor Environmental Parameter Monitoring System”.
- Preserves the last ESP32 IP address and automatically reconnects on startup.
- Changing the IP address requires tapping **Connect** before the polling target changes.

### v1.0.0

- Initial Kotlin and Jetpack Compose ESP32 sensor monitoring application.
- Added HTTP polling for temperature, humidity, illuminance, PM1.0, PM2.5 and PM10.

## License / 许可证

This repository is an academic FYP project. / 本仓库用于毕业设计项目。
