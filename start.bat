@echo off
title Seckill - Starting...

echo ============================================
echo  Building Frontend...
echo ============================================
cd frontend
call npm run build
if %errorlevel% neq 0 (
    echo [FAIL] Frontend build failed!
    pause
    exit /b 1
)
cd ..
echo [OK] Frontend built to frontend/dist
echo.

echo ============================================
echo  Starting Docker containers...
echo ============================================
docker compose up -d
if %errorlevel% neq 0 (
    echo [FAIL] Docker containers failed to start!
    pause
    exit /b 1
)
echo [OK] Containers started
echo.

echo ============================================
echo  Waiting for containers...
echo ============================================
timeout /t 5 /nobreak >nul

echo ============================================
echo  Starting Backend (Spring Boot :8080)
echo ============================================
start "Seckill-Backend" cmd /k "cd backend && mvn spring-boot:run"

echo.
echo ============================================
echo  All services started!
echo.
echo  Website : http://localhost
echo  Swagger : http://localhost/doc.html
echo  Backend : http://localhost:8080
echo ============================================
pause
