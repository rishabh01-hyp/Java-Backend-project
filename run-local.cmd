@echo off
setlocal
cd /d "%~dp0"
if not exist "lib\mysql-connector-j-9.2.0.jar" (
    echo Missing lib\mysql-connector-j-9.2.0.jar. See README.md.
    exit /b 1
)
javac --release 17 -d out Main.java Shop.java Database.java Csv.java
if errorlevel 1 exit /b 1
java -cp "out;lib/*" Main
exit /b %errorlevel%
