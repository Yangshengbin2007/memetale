# 解决 macOS 安全警告

## 问题
当双击 `run.command` 时，macOS 显示：
> "Apple could not verify 'run.command' is free of malware..."

## 解决方案（按推荐顺序）

### 方法 1：右键打开（最简单）⭐

1. **不要直接双击**
2. **右键点击** `run.command` 文件
3. 选择 **"打开"** (Open)
4. 如果再次提示，点击 **"打开"** 按钮
5. 之后就可以正常双击运行了

### 方法 2：在终端中运行（推荐用于展示）

打开终端（Terminal），运行：

```bash
cd /Users/415330/Documents/GitHub/Beather/Jerry-advprogramming2026-Individual-project-Repo
./run.command
```

### 方法 3：移除隔离属性（永久解决）

在终端中运行：

```bash
cd /Users/415330/Documents/GitHub/Beather/Jerry-advprogramming2026-Individual-project-Repo
xattr -d com.apple.quarantine run.command
```

如果提示 "No such xattr"，说明文件没有被隔离，使用**方法 1** 或 **方法 2**。

### 方法 4：系统设置允许（不推荐）

1. 打开 **系统设置** (System Settings)
2. 进入 **隐私与安全性** (Privacy & Security)
3. 在 **安全性** (Security) 部分
4. 找到被阻止的应用，点击 **"仍要打开"** (Open Anyway)

## 🎮 展示时建议

**最佳方案：使用终端运行**
- 更专业
- 可以看到编译和运行过程
- 避免安全警告

**快速命令：**
```bash
cd ~/Documents/GitHub/Beather/Jerry-advprogramming2026-Individual-project-Repo && ./run.command
```

## 为什么会出现这个警告？

macOS 的 Gatekeeper 会检查：
- 文件是否来自未知开发者
- 文件是否从网络下载（如 GitHub）
- 文件是否有数字签名

这是正常的安全机制，不是错误。
