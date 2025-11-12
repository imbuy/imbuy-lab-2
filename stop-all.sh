#!/bin/bash

set -e

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

SERVICES=("bid-service" "lot-service" "category-service" "auth-service" "user-service" "api-gateway" "config-server" "discovery-server")

for service in "${SERVICES[@]}"; do
    PID_FILE="$BASE_DIR/logs/$service.pid"
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "Остановка $service (PID: $PID)..."
            kill "$PID" 2>/dev/null || true
            rm "$PID_FILE"
            echo "$service остановлен"
        else
            echo "$service уже не запущен"
            rm "$PID_FILE"
        fi
    fi
done

read -p "Остановить базы данных Docker? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Остановка баз данных..."
    cd "$BASE_DIR"
    docker-compose down
    echo "Базы данных остановлены"
fi