@echo off
setlocal
cd /d "%~dp0"
if not exist "target\repair-shop-2.0.0.jar" (
    call mvn package
    if errorlevel 1 exit /b 1
)
java -jar "target\repair-shop-2.0.0.jar" --local
exit /b %errorlevel%
