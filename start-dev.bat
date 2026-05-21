@echo off
:: Fail fast if the configured development database is unavailable.
call mvn -Dtest=DatabaseConnectionTest test
if errorlevel 1 (
    echo Database connection check failed. Application will not be started.
    pause
    exit /b 1
)

:: Use 'call' so that the script does not abort after Maven
call mvn clean package

:: Check if the build was successful
if errorlevel 1 (
    echo Build failed!
    pause
    exit /b 1
)

:: The file is located in the 'target' folder by default
java -jar target\stock-tracker-1.0.0.jar

::pause
