package com.kalidroid.hardware

class WifiController {
    fun scan(): HardwareResult = HardwareResult(false, "Wi-Fi 扫描已关闭。")

    fun inject(payload: String): HardwareResult {
        return HardwareResult(false, "拒绝 Wi-Fi 注入。payload 长度=${payload.length}")
    }
}
