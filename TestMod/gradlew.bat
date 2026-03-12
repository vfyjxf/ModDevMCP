@echo off
set DIR=%~dp0
call "%DIR%..\gradlew.bat" -p "%DIR:~0,-1%" %*
