# assets/proot 放置说明

v2.1 容器引擎启动时自动从 `assets/proot` 释放 proot 二进制到 `filesDir/bin/proot`。

## 需要放置的文件

```
app/src/main/assets/proot          # proot 静态可执行文件（arm64）
```

## 获取方式（任选其一）

1. **Termux 源**（推荐）
   ```bash
   # 在 Termux 中执行
   pkg install proot
   cp $(which proot) <工程>/app/src/main/assets/proot
   ```

2. **GitHub Release**（Termux proot 已构建产物）
   - https://github.com/termux/proot/releases
   - 下载 aarch64 版本，重命名为 `proot`

3. **proot-distro 同源**
   - 从已安装 proot-distro 的设备拷贝 `/data/data/com.termux/files/usr/bin/proot`

## 注意事项

- 必须是 **静态编译**（不依赖 Termux 的 .so），Android app 进程才能直接执行
- 放置后 APK 体积约增加 1-2MB
- 若不放此文件，容器启动时会报 "缺少 proot：请先从 assets 安装 proot"，其余功能不受影响

## 验证

```bash
# Linux 环境交叉验证产物
file proot          # 应显示 ELF 64-bit LSB executable, ARM aarch64
```