#!/bin/bash
# Mac double-click launcher: double-click this file in Finder to run the game in Terminal.
# First time: right-click -> Open (or System Preferences -> Security allow it).

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Ensure output directory exists
if [ ! -d out ]; then
    mkdir -p out
fi

echo "========================================="
echo "  memetale RPG - Game Launcher"
echo "========================================="
echo ""
echo "Working directory: $SCRIPT_DIR"
echo ""

# Find Java home (prefer Java 18 if available, otherwise use default)
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

# Clean old compiled files to avoid version mismatch
echo "Cleaning old compiled files..."
rm -rf out/game
echo "✅ Cleaned"
echo ""

# Compile the game (use same Java version for compile and run)
echo "Compiling Java files..."
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
    echo "❌ Compilation failed!"
    echo ""
    read -p "Press Enter to close..."
    exit 1
fi

echo "✅ Compilation successful!"
echo ""
echo "Starting game..."
echo ""

# Run the game (use same Java version)
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
    echo "Game exited with error code: $EXIT_CODE"
fi
echo ""
read -p "Press Enter to close..."
exit $EXIT_CODE
