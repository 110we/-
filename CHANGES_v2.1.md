# KaliDroid v2.1 迭代变更说明

> 基于两代工程（Kali_Toolbox v1.0.0 + KaliDroid Ops v1.0.0）合并迭代。
> 架构文档见 `ARCHITECTURE_v2.1.md`

---

## 本次变更清单

### 1. 容器引擎（从锁定桩 → 真实现）

| 文件 | 变更 |
|---|---|
| `kali/KaliContainer.kt` | 重写：proot 引擎、start/stop/exec/状态机、assets proot 释放 |
| `kali/RootfsManager.kt` | 重写：内部流式 Tar 解压（tar/tar.gz/tgz），不依赖系统 tar |
| `kali/MountManager.kt` | 重写：proot 模式虚拟映射，提供 prootBindArgs() |

### 2. 权限执行器（从锁定桩 → 条件解锁）

| 文件 | 变更 |
|---|---|
| `permission/AdbExecutor.kt` | 重写：Shizuku binder 通道，反射调用，无硬依赖 |
| `permission/RootExecutor.kt` | 重写：su -c 执行，超时+截断+检测 |
| `permission/PermissionManager.kt` | 解锁：ADB/ROOT 可用时启用，不可用自动降级 NORMAL |
| `utils/ShizukuUtils.kt` | 重写：检测 + 授权入口 |

### 3. 新增审计

| 文件 | 说明 |
|---|---|
| `utils/AuditLogger.kt` | 行式 JSON 审计日志，落盘 filesDir/audit/ |

### 4. UI / 入口适配

| 文件 | 变更 |
|---|---|
| `KaliDroidApp.kt` | 新容器构造 + 启动时自动安装 assets/proot |
| `ui/settings/SettingsScreen.kt` | 权限切换真实化、容器 exec 显示真实输出 |
| `PermissionChooserActivity.kt` | 移除强制回退 NORMAL，持久化用户选择 |
| `bridge/NativeExecutorBridge.kt` | 执行结果接入审计 |

### 5. 后端代理

| 文件 | 变更 |
|---|---|
| `backend/server.js` | kali 接口从"禁用"改为"代理到原生侧" |

### 6. 资源占位

```
app/src/main/assets/PLACE_PROOT_HERE.md   # 放置说明（proot 二进制的落点）
```

---

## 编译前必做

把 proot 静态二进制放到 `app/src/main/assets/proot`（获取方式见 PLACE_PROOT_HERE.md）。
不放也能编译，只是容器启动会提示缺 proot。

## 编译

```bash
./gradlew :app:assembleDebug
# 产物: app/build/outputs/apk/debug/app-debug.apk
```

## 验证路径

1. App 启动 → 设置页
2. `容器：start()` → 若已放 proot + 已导入 rootfs，显示"Kali 容器运行中"
3. `容器：exec(uname)` → 显示容器内 uname 输出
4. 权限切换：NORMAL / ADB(Shizuku) / ROOT(su) 按检测结果启用或降级
5. `filesDir/audit/audit-YYYYMMDD.log` 有审计记录