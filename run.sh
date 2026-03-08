#!/bin/bash
# Run from project root: ./run.sh
# Or make executable once: chmod +x run.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

if [ ! -d out ]; then
    mkdir out
fi

echo "Compiling..."
javac -encoding UTF-8 -d out -sourcepath src src/game/launcher/Main.java
if [ $? -ne 0 ]; then
    echo "Compile failed."
    exit 1
fi

echo "Running game..."
java -cp out game.launcher.Main
exit $?
