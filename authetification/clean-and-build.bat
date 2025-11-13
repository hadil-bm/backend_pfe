@echo off
echo Cleaning and building the project...
call mvnw.cmd clean compile
if %ERRORLEVEL% EQU 0 (
    echo Build successful!
    echo Running the application...
    call mvnw.cmd spring-boot:run
) else (
    echo Build failed! Please check the errors above.
    pause
)
