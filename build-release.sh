#!/bin/bash
# Build downloadable executable jar: dist/memetale-rpg.jar

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

if [ ! -d out ]; then
    mkdir -p out
fi
if [ ! -d dist ]; then
    mkdir -p dist
fi

# Prefer Java 18 if available
JAVA_HOME_18="/Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home"
if [ -d "$JAVA_HOME_18" ]; then
    export JAVA_HOME="$JAVA_HOME_18"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

JAVAC_BIN="${JAVA_HOME:-}/bin/javac"
JAR_BIN="${JAVA_HOME:-}/bin/jar"

echo "Cleaning old outputs..."
rm -rf out/game
rm -f dist/memetale-rpg.jar

echo "Compiling..."
if [ -x "$JAVAC_BIN" ]; then
    "$JAVAC_BIN" -encoding UTF-8 --release 18 -d out -sourcepath src \
        src/game/launcher/Main.java src/game/model/forest/ForestMapData.java src/game/model/forest/TrollCaveData.java
else
    javac -encoding UTF-8 --release 18 -d out -sourcepath src \
        src/game/launcher/Main.java src/game/model/forest/ForestMapData.java src/game/model/forest/TrollCaveData.java
fi

echo "Packaging jar..."
if [ -x "$JAR_BIN" ]; then
    "$JAR_BIN" cfe dist/memetale-rpg.jar game.launcher.Main -C out .
else
    jar cfe dist/memetale-rpg.jar game.launcher.Main -C out .
fi

echo "Done: dist/memetale-rpg.jar"
echo "Run with: java -jar dist/memetale-rpg.jar"
