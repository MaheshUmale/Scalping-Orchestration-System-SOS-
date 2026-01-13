@echo off
title SOS Engine
echo Starting SOS Engine...
cd sos-engine
REM Assumes the project has been built with Maven (mvn package)
if not exist "target\sos-engine-1.0-SNAPSHOT.jar" (
    echo "Error: sos-engine-1.0-SNAPSHOT.jar not found."
    echo "Please build the project first using 'mvn package'."
    pause
    exit /b
)
java -jar target\sos-engine-1.0-SNAPSHOT.jar
echo.
echo SOS Engine has stopped.
pause
