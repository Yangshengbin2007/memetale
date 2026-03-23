@echo off
REM Windows 启动器；英文提示请用 run-en.bat
chcp 65001 >nul
cd /d "%~dp0"
if not exist out mkdir out
echo 正在编译…
javac -encoding UTF-8 --release 18 -d out -sourcepath src src\game\launcher\Main.java src\game\model\forest\ForestMapData.java src\game\model\forest\TrollCaveData.java
if errorlevel 1 (
    echo 编译失败。
    pause
    exit /b 1
)
echo 正在启动游戏…
java -cp out game.launcher.Main
if errorlevel 1 pause
