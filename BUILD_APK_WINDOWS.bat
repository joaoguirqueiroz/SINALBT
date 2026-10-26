@echo off
setlocal
cd /d "%~dp0"
where gradle >nul 2>&1
if errorlevel 1 (
  echo [ERRO] Gradle 8.9 nao encontrado no PATH.
  echo Use Android Studio ou execute o workflow em GitHub Actions.
  exit /b 1
)
call gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
if errorlevel 1 (
  echo [ERRO] Compilacao falhou. Verifique as mensagens acima.
  exit /b 1
)
if exist "app\build\outputs\apk\debug\app-debug.apk" (
  echo [OK] APK: app\build\outputs\apk\debug\app-debug.apk
) else (
  echo [ERRO] Compilacao finalizada sem APK verificado.
  exit /b 1
)
