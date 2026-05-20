@echo off
:: Use 'call' so that the script does not abort after Maven
call mvn clean package

:: Check if the build was successful
if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    pause
    exit /b %ERRORLEVEL%
)

:: The file is located in the 'target' folder by default
java -jar target\stock-tracker-1.0.0.jar

::pause
