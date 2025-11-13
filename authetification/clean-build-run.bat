@echo off
echo Cleaning project...
call mvnw.cmd clean

echo.
echo Compiling project...
call mvnw.cmd compile

echo.
echo Building project...
call mvnw.cmd package -DskipTests

echo.
echo Build completed! You can now run the application with:
echo java -jar target\authetification-0.0.1-SNAPSHOT.jar
echo.
echo Or use: mvnw.cmd spring-boot:run

pause

