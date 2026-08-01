@rem Gradle startup script for Windows
@rem Usage: gradlew [task], e.g. gradlew build

@if "%DEBUG%"=="" @echo off
@rem Set local scope
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve "." and ".." in APP_HOME
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem ---- Find Java ----
@rem Check JAVA_HOME first, then try PATH

if defined JAVA_HOME goto :findJavaFromJavaHome

@rem Try java on PATH
set JAVA_EXE=java.exe
"%JAVA_EXE%" -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto :execute

@rem Try common JDK locations
if exist "C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot\bin\java.exe" (
    set JAVA_EXE=C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot\bin\java.exe
    goto :execute
)

echo ERROR: Cannot find Java. Please install Java 21. >&2
exit /b 1

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%\bin\java.exe

if exist "%JAVA_EXE%" goto :execute
echo ERROR: JAVA_HOME is set but no java.exe found at %JAVA_EXE% >&2
exit /b 1

:execute
@rem ---- Run Gradle ----
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
if "%OS%"=="Windows_NT" endlocal

:omega
