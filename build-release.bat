@echo off
REM Build downloadable executable jar: dist\memetale-rpg.jar
chcp 65001 >nul
cd /d "%~dp0"

if not exist out mkdir out
if not exist dist mkdir dist

echo Cleaning old outputs...
rmdir /s /q out\game 2>nul
del /q dist\memetale-rpg.jar 2>nul

echo Compiling...
javac -encoding UTF-8 --release 18 -d out -sourcepath src src\game\launcher\Main.java src\game\model\forest\ForestMapData.java src\game\model\forest\TrollCaveData.java
if errorlevel 1 (
    echo Compile failed.
    pause
    exit /b 1
)

echo Packaging jar...
jar cfe dist\memetale-rpg.jar game.launcher.Main -C out .
if errorlevel 1 (
    echo Jar packaging failed.
    pause
    exit /b 1
)

echo Done: dist\memetale-rpg.jar
echo Run with: java -jar dist\memetale-rpg.jar
