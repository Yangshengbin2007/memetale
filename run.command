#!/bin/bash
# macOS：在访达中双击本文件，会在终端中编译并启动游戏。
# 首次运行若被拦截：右键本文件 →「打开」，或见 README「macOS 安全与权限」。
# English launcher: use run-en.command

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

if [ ! -d out ]; then
    mkdir -p out
fi

echo "========================================="
echo "  memetale RPG — 启动器"
echo "========================================="
echo ""
echo "工作目录: $SCRIPT_DIR"
echo ""

JAVA_HOME_18="/Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home"
if [ -d "$JAVA_HOME_18" ]; then
    export JAVA_HOME="$JAVA_HOME_18"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "使用 Java 18: $JAVA_HOME"
    echo ""
else
    echo "使用系统默认 Java"
    echo ""
fi

echo "正在清理旧编译文件…"
rm -rf out/game
echo "完成。"
echo ""

echo "正在编译 Java…"
JAVAC_BIN="${JAVA_HOME:-}/bin/javac"
if [ -x "$JAVAC_BIN" ]; then
    "$JAVAC_BIN" -encoding UTF-8 --release 18 -d out -sourcepath src \
        src/game/launcher/Main.java src/game/model/forest/ForestMapData.java src/game/model/forest/TrollCaveData.java
else
    javac -encoding UTF-8 --release 18 -d out -sourcepath src \
        src/game/launcher/Main.java src/game/model/forest/ForestMapData.java src/game/model/forest/TrollCaveData.java
fi
if [ $? -ne 0 ]; then
    echo ""
    echo "编译失败。"
    echo ""
    read -p "按回车键关闭窗口…"
    exit 1
fi

echo "编译成功。"
echo ""
echo "正在启动游戏…"
echo ""

JAVA_BIN="${JAVA_HOME:-}/bin/java"
if [ -x "$JAVA_BIN" ]; then
    "$JAVA_BIN" -cp out game.launcher.Main
else
    java -cp out game.launcher.Main
fi
EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo "游戏已正常退出。"
else
    echo "游戏退出，代码: $EXIT_CODE"
fi
echo ""
read -p "按回车键关闭窗口…"
exit $EXIT_CODE
