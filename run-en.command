#!/bin/bash
# English macOS double-click launcher (Terminal opens, compiles, runs the game).
# First launch: Right-click this file → Open. If blocked, see README → macOS security.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

if [ ! -d out ]; then
    mkdir -p out
fi

echo "========================================="
echo "  memetale RPG — launcher"
echo "========================================="
echo ""
echo "Working directory: $SCRIPT_DIR"
echo ""

JAVA_HOME_18="/Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home"
if [ -d "$JAVA_HOME_18" ]; then
    export JAVA_HOME="$JAVA_HOME_18"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "Using Java 18: $JAVA_HOME"
    echo ""
else
    echo "Using system default Java"
    echo ""
fi

echo "Cleaning old build output..."
rm -rf out/game
echo "Done."
echo ""

echo "Compiling..."
JAVAC_BIN="${JAVA_HOME:-}/bin/javac"
if [ -x "$JAVAC_BIN" ]; then
    "$JAVAC_BIN" -encoding UTF-8 --release 18 -d out -sourcepath src \
        src/game/launcher/Main.java src/game/model/forest/TrollCaveData.java
else
    javac -encoding UTF-8 --release 18 -d out -sourcepath src \
        src/game/launcher/Main.java src/game/model/forest/TrollCaveData.java
fi
if [ $? -ne 0 ]; then
    echo ""
    echo "Compilation failed."
    echo ""
    read -p "Press Enter to close..."
    exit 1
fi

echo "Compilation OK."
echo ""
echo "Starting game..."
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
    echo "Game exited normally."
else
    echo "Game exited with code: $EXIT_CODE"
fi
echo ""
read -p "Press Enter to close..."
exit $EXIT_CODE
