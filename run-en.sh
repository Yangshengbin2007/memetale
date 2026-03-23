#!/bin/bash
# English launcher — same behavior as run.sh (Chinese UI). Run from project root: ./run-en.sh
# Make executable once: chmod +x run-en.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

if [ ! -d out ]; then
    mkdir out
fi

echo "Compiling..."

# Prefer Java 18 for compile and run to avoid class-version mismatch
JAVA_HOME_18="/Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home"
if [ -d "$JAVA_HOME_18" ]; then
    export JAVA_HOME="$JAVA_HOME_18"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "Using Java 18: $JAVA_HOME"
else
    echo "Using system default Java (ensure compile and run use the same major version)."
fi

echo "Cleaning old build output..."
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
    echo "Compile failed."
    exit 1
fi

echo "Starting game..."
if [ -x "$JAVA_BIN" ]; then
    "$JAVA_BIN" -cp out game.launcher.Main
else
    java -cp out game.launcher.Main
fi
exit $?
