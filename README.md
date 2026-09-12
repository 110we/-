# KaliDroid Ops

执行链路按架构图落地：

Vite + React UI → WebSocket → Node.js 后端
- 调用系统命令 → Android Shell / ADB（白名单）
- 或经 WebSocket → Android 原生执行器 → Root / ADB / Normal（Root/ADB 锁定，Normal 可执行）

```
app/src/main/java/com/kalidroid/
├── KaliDroidApp.kt
├── MainActivity.kt
├── PermissionChooserActivity.kt
├── permission/          CommandExecutor + Root/Adb/Normal
├── kali/                Container / Rootfs / Mount（锁定）
├── terminal/            TerminalActivity + TerminalView
├── ui/                  dashboard / tools / settings / theme
├── workflow/
├── hardware/            只读拒绝
├── bridge/              NativeExecutorBridge.kt
├── jni/
├── model/
└── utils/
```

## Web 预览

```bash
cd backend && npm install && npm run dev
cd frontend && npm install && npm run dev
```

设置里可切换两条通道。原生执行器需在 Android 设置页连接 `ws://<host>:3001/ws`。

## 编译 APK

本仓库已带 Gradle Wrapper。本机无 JDK/SDK 时，用 GitHub Actions 出包：

1. 把完整源码推到 `origin/main`
2. 打开仓库 Actions，跑 `Android CI`
3. 从 Artifacts 下载 `kalidroid-ops-debug`

本地有 Android Studio 时：

```bash
./gradlew :app:assembleDebug
```

产物路径：`app/build/outputs/apk/debug/app-debug.apk`
