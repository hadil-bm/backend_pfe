@echo off
echo ============================================
echo Starting Spring Boot Application
echo ============================================
echo.

cd /d "%~dp0"

echo Starting application with Spring Boot Maven Plugin...
echo.
call mvnw.cmd spring-boot:run

pause

