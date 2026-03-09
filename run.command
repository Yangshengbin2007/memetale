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

# Compile the game
echo "Compiling Java files..."
javac -encoding UTF-8 -d out -sourcepath src src/game/launcher/Main.java
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

# Run the game
java -cp out game.launcher.Main
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
