#!/bin/bash
# Mac double-click launcher: double-click this file in Finder to run the game in Terminal.
# First time: right-click -> Open (or System Preferences -> Security allow it).

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

if [ ! -d out ]; then
    mkdir out
fi

echo "Compiling..."
javac -encoding UTF-8 -d out -sourcepath src src/game/launcher/Main.java
if [ $? -ne 0 ]; then
    echo "Compile failed."
    read -p "Press Enter to close..."
    exit 1
fi

echo "Running game..."
java -cp out game.launcher.Main
echo ""
read -p "Press Enter to close..."
exit $?
