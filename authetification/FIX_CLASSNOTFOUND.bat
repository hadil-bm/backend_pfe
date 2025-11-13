@echo off
echo ============================================
echo Fixing ClassNotFoundException Issue
echo ============================================
echo.

cd /d "%~dp0"

echo Step 1: Cleaning project...
call mvnw.cmd clean
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Clean failed!
    pause
    exit /b 1
)

echo.
echo Step 2: Compiling project...
call mvnw.cmd compile
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)

echo.
echo Step 3: Packaging project...
call mvnw.cmd package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Packaging failed!
    pause
    exit /b 1
)

echo.
echo ============================================
echo Build completed successfully!
echo ============================================
echo.
echo To run the application, use one of these methods:
echo 1. mvnw.cmd spring-boot:run
echo 2. java -jar target\authetification-0.0.1-SNAPSHOT.jar
echo.
pause

