@echo off
title Seckill - Stopping...

echo ============================================
echo  Stopping Backend...
echo ============================================
taskkill /fi "WINDOWTITLE eq Seckill-Backend*" /f 2>nul
echo [OK] Backend stopped
echo.

echo ============================================
echo  Stopping Docker containers...
echo ============================================
docker compose down
if %errorlevel% neq 0 (
    echo [FAIL] Docker containers failed to stop!
    pause
    exit /b 1
)
echo [OK] All containers stopped
echo.
pause
