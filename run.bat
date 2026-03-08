@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist out mkdir out
echo Compiling...
javac -encoding UTF-8 -d out -sourcepath src src\game\launcher\Main.java
if errorlevel 1 (
    echo Compile failed.
    pause
    exit /b 1
)
echo Running game...
java -cp out game.launcher.Main
if errorlevel 1 pause
