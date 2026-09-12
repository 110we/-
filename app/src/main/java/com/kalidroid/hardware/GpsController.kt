package com.kalidroid.hardware

class GpsController {
    fun spoof(lat: Double, lon: Double): HardwareResult {
        return HardwareResult(false, "拒绝模拟位置: $lat,$lon")
    }

    fun read(): HardwareResult = HardwareResult(false, "GPS 采集已关闭。")
}
