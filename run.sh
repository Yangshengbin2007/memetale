#!/bin/bash
# 在项目根目录执行: ./run.sh
# 首次需赋予执行权限: chmod +x run.sh
# 英文界面脚本请用: ./run-en.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

if [ ! -d out ]; then
    mkdir out
fi

echo "正在编译…"

# 优先使用 Java 18，避免编译与运行 class 版本不一致
JAVA_HOME_18="/Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home"
if [ -d "$JAVA_HOME_18" ]; then
    export JAVA_HOME="$JAVA_HOME_18"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "使用 Java 18: $JAVA_HOME"
else
    echo "使用系统默认 Java（请确保编译与运行使用同一主版本）。"
fi

echo "正在清理旧的编译输出…"
rm -rf out/game

JAVAC_BIN="${JAVA_HOME:-}/bin/javac"
JAVA_BIN="${JAVA_HOME:-}/bin/java"
if [ -x "$JAVAC_BIN" ]; then
    "$JAVAC_BIN" -encoding UTF-8 --release 18 -d out -sourcepath src \
        src/game/launcher/Main.java src/game/model/forest/ForestMapData.java src/game/model/forest/TrollCaveData.java
else
    javac -encoding UTF-8 --release 18 -d out -sourcepath src \
        src/game/launcher/Main.java src/game/model/forest/ForestMapData.java src/game/model/forest/TrollCaveData.java
fi
if [ $? -ne 0 ]; then
    echo "编译失败。"
    exit 1
fi

echo "正在启动游戏…"
if [ -x "$JAVA_BIN" ]; then
    "$JAVA_BIN" -cp out game.launcher.Main
else
    java -cp out game.launcher.Main
fi
exit $?
