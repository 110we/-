# KaliDroid 统一架构 v2.1

> 由两代工程提炼迭代：
> - **Kali_Toolbox v1.0.0**（com.kali.toolbox）：本地 Kali 容器引擎，容器链路完整但 assets 缺 proot、解压依赖系统 tar
> - **KaliDroid Ops v1.0.0**（com.kalidroid）：三端远程工作台框架完整，但容器/高权限全部锁定桩

---

## 1. 两代工程核心架构提炼

### 1.1 Kali_Toolbox v1.0.0（本地容器愿景）

```
Android App (Compose)
├── AppContainer ── 持有 KaliContainer / SensorController / ReportGenerator
│   └── installProotFromAssets()  ← assets/proot (缺失!)
├── KaliContainer ── start/stop/exec/installProot/importRootfs/analyze/pcap/doctor
│   └── untar() ← 依赖系统 tar (5个候选路径, Android 大多没有)
├── ContainerEnvironment ── ABI探测 / proot解析 / rootfs检查 / doctor
├── ContainerInfo ── state/distro/rootfsPath/prootPath/issues (StateFlow)
├── ToolCatalog ── 工具目录 (search/distribution/special)
├── CaptureService ── VpnService 流量捕获 (pcap)
└── NativeExecutorBridge ── WebSocket 桥
```

**优点**：容器引擎实现完整（状态机/proot探测/rootfs导入/诊断/捕获）
**断点**：proot 未进 assets、untar 依赖系统 tar → 容器永远起不来

### 1.2 KaliDroid Ops v1.0.0（远程安全工作台）

```
Vite+React UI ──WebSocket──→ Node.js Hub (:3001) ──双通道──→ Android
                              │                          ├─ shell: 白名单 execFile
                              └── REST API                └─ native: WS → 原生执行器
                                                            ├─ NormalExecutor (白名单)
                                                            ├─ AdbExecutor  (锁定桩)
                                                            └─ RootExecutor (锁定桩)
Android 端:
├── PermissionManager ── NORMAL/ADB/ROOT 三模式 (ADB/ROOT 强制回退 NORMAL)
├── KaliContainer ── 禁用桩 (start 返回"已禁用")
├── RootfsManager ── 拒绝桩
├── MountManager ── 锁定桩
├── NativeExecutorBridge ── WS 客户端 (hello/exec/result)
├── WorkflowEngine + WorkflowParser
└── native-lib.cpp (JNI) ── version/hostFingerprint/denyPrivilegedCall
```

**优点**：三端框架完整、传输协议清晰、权限分级抽象、白名单双保险
**断点**：容器/高权限全部锁定 → 只有只读白名单命令可用，无实战能力

---

## 2. 优化迭代方向（合并两者优势）

| 维度 | 取自 Kali_Toolbox | 取自 KaliDroid Ops | 迭代优化 |
|---|---|---|---|
| 容器引擎 | ✅ 完整实现 | ❌ 桩 | 修复断点后复用 |
| 三端框架 | ❌ 无 | ✅ 完整 | 保留 |
| 权限分级 | ❌ 单一 | ✅ NORMAL/ADB/ROOT | 桩 → 真实现 |
| 传输协议 | WS 单点 | ✅ 双通道+Hub | 扩展流式输出 |
| 工具目录 | ✅ ToolCatalog | ✅ 前端+后端双份 | 统一数据源 |
| 捕获能力 | ✅ CaptureService | ❌ 硬件全关 | 回归为可选模块 |
| 工作流 | ❌ 无 | ✅ WorkflowEngine | 保留扩展 |
| 审计 | ❌ 无 | ⚠️ 白名单拦截 | 补完整审计日志 |

---

## 3. 新架构 v2.1

### 3.1 分层总览

```
┌──────────────────────────────────────────────────────┐
│ 表现层                                                │
│   Android Compose App (原生)  ↔  Web React Console   │
│   仪表盘 / 工具库 / 终端 / 容器控制台 / 设置          │
└────────────────────────┬─────────────────────────────┘
                         │ WebSocket + REST（统一协议 v1）
┌────────────────────────▼─────────────────────────────┐
│ 控制层 Node.js Hub (:3001)                            │
│   会话管理 / 鉴权 / 消息路由 / 广播 / 审计汇聚        │
│   REST: /api/*   WS: /ws (ui | android 双角色)        │
└────────────────────────┬─────────────────────────────┘
                         │ 双通道
┌────────────────────────▼─────────────────────────────┐
│ 服务层 Android 原生                                   │
│   PermissionManager (分级真实现)                      │
│   KaliContainer (proot 引擎)                          │
│   WorkflowEngine / AuditLogger / ToolCatalog          │
│   CaptureModule (可选授权)                            │
└────────────────────────┬─────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────┐
│ 能力层                                                │
│   proot + rootfs (容器)                               │
│   白名单 exec (NORMAL)                                │
│   Shizuku exec (ADB, 授权后)                          │
│   su exec (ROOT, 授权后)                              │
│   pcap 捕获 (授权后)                                  │
└──────────────────────────────────────────────────────┘
```

### 3.2 容器引擎（修复版）

```
RootfsManager（真实现）
├── 来源: assets 内置 base / 首次启动下载 / 手动导入
├── 解压: Java TarArchiveInputStream (tar/tar.gz/tar.xz)
│         └─ 不依赖系统 tar，绕开 Android 无 tar 问题
├── 校验: SHA256 + 版本号
└── 预检: 空间 / 权限 / 已存在版本

KaliContainer（proot 引擎，非 chroot）
├── 启动: proot -0 -r rootfs -b /proc -b /dev -b /sys -b /sdcard
├── proot 来源: assets/proot（静态编译，随 APK 分发）
├── 状态机: EXITED/STARTING/RUNNING/STOPPING/FAILED/MISSING_ROOTFS/MISSING_PROOT
├── exec: 超时 / 输出截断 / 并发控制
└── shellOf: /bin/bash → /bin/sh → /usr/bin/bash 探测

MountManager（proot 场景降级）
├── proot 模式: 无需真 mount，用 -b 参数模拟
└── root 模式: 可选 chroot / mount 真挂载（用户显式授权）
```

### 3.3 权限分级（真实现）

```
NORMAL ── 白名单命令（默认, 无依赖）
ADB    ── Shizuku 授权后启用（已有 ShizukuUtils 检测 → 补授权流程）
ROOT   ── su 检测 + 用户显式确认后启用
                │
                ├─ 每级: denied 标记 / 审计 / 超时 / 截断
                └─ 降级: 高权限不可用 → 自动回退 NORMAL + notice
```

### 3.4 统一协议 v1（扩展自 KaliDroid Ops）

```jsonc
// 新增消息类型
{ "type": "exec_stream", "id": "...", "command": "...", "mode": "NORMAL" }
{ "type": "stream_chunk", "id": "...", "data": "..." }        // 终端流式
{ "type": "container/start", "id": "...", "distro": "kali-rolling" }
{ "type": "container/result", "id": "...", "state": "RUNNING", "pid": 1234 }
{ "type": "audit", "event": "exec", "user": "...", "cmd": "...", "mode": "...", "ok": true }
```

### 3.5 安全边界（保留并强化）

| 层 | 机制 |
|---|---|
| 命令白名单 | 服务端 ALLOWED + 客户端 allowlist 双保险 |
| 审计 | AuditLogger 落盘: 时间/用户/命令/模式/结果/哈希 |
| rootfs | SHA256 + 版本号 + 导入白名单目录 |
| 资源 | execTimeout / maxOutput / 并发上限 |
| 硬件 | 默认全关，逐项用户授权（回归 CaptureModule） |

### 3.6 工作流（保留扩展）

```
WorkflowEngine
├── 步骤: 白名单命令 / 容器 exec / 工具目录调用
├── 分支: on_success / on_error
├── 触发: 手动 / 定时 / 语音
└── 审计: 每步落日志
```

---

## 4. 落地路线（迭代顺序）

| 阶段 | 内容 | 工作量 |
|---|---|---|
| **P0** | proot 打包进 assets + Java 解压 rootfs + 容器跑通 `whoami/uname` | 1-2 天 |
| **P1** | Shizuku ADB 解锁 + ROOT 条件解锁（权限桩补全） | 2-3 天 |
| **P2** | CaptureModule 回归（pcap 捕获, 授权制） + 审计日志 | 2-3 天 |
| **P3** | Web 容器控制台（终端流式输出 + rootfs 管理） | 3-5 天 |
| **P4** | 工作流扩展 + 工具目录统一数据源 + 云同步 | 按需 |

---

## 5. 关键决策点（需团队确认）

1. **rootfs 分发**：内置(APK 膨胀) vs 首次下载 vs 手动导入 → 建议 P0 手动导入 + 后续下载
2. **proot 许可**：assets 内置需确认 proot 静态编译产物（Termux 源）合规性
3. **ROOT/ADB 默认值**：建议默认 NORMAL，高级通道首次使用时弹授权
4. **容器 vs 工具库优先级**：P0 容器跑通 vs P1 权限解锁，业务优先级定先后
