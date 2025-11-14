#!/bin/bash

set -e

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if ! command -v docker &> /dev/null; then
    echo "Ошибка: Docker не установлен"
    exit 1
fi

echo "Запуск бд"
cd "$BASE_DIR"
docker-compose up -d
echo "Базы данных запущены"
sleep 10

start_service() {
    local service_name=$1
    local service_dir="$BASE_DIR/$service_name"
    
    if [ ! -d "$service_dir" ]; then
        return
    fi
    
    echo "Запуск $service_name..."
    cd "$service_dir"
    mvn spring-boot:run > "$BASE_DIR/logs/$service_name.log" 2>&1 &
    echo $! > "$BASE_DIR/logs/$service_name.pid"
    echo "$service_name запущен (PID: $(cat "$BASE_DIR/logs/$service_name.pid"))"
    sleep 5
}

mkdir -p "$BASE_DIR/logs"

echo "Запуск сервисов"
echo ""

start_service "discovery-server"
start_service "config-server"
start_service "api-gateway"
start_service "user-service"
start_service "category-service"
start_service "lot-service"
start_service "bid-service"

echo ""
echo "========================================="
echo "Все сервисы запущены"
echo "========================================="
echo ""
echo "Логи находятся в директории: $BASE_DIR/logs"
echo ""
echo "Проверьте статус сервисов:"
echo "- Eureka: http://localhost:8761"
echo "- Config Server: http://localhost:8888"
echo "- API Gateway: http://localhost:8080"
echo ""
echo "Для остановки всех сервисов выполните: ./stop-all.sh"
echo ""

