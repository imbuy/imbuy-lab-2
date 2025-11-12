#!/bin/bash

set -e

BASE_URL="http://localhost:8080"
CONFIG_URL="http://localhost:8888"
EUREKA_URL="http://localhost:8761"

echo "========================================="
echo "Тестирование микросервисов ImBuy"
echo "========================================="
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_service() {
    local name=$1
    local url=$2
    
    echo -n "Проверка $name... "
    if curl -s -f -o /dev/null "$url" 2>/dev/null; then
        echo -e "${GREEN}✓ OK${NC}"
        return 0
    else
        echo -e "${RED}✗ FAILED${NC}"
        return 1
    fi
}

check_json() {
    local name=$1
    local url=$2
    
    echo -n "Проверка $name... "
    response=$(curl -s "$url" 2>/dev/null)
    if [ $? -eq 0 ] && [ ! -z "$response" ] && [[ "$response" == *"{"* ]] || [[ "$response" == *"["* ]]; then
        echo -e "${GREEN}✓ OK${NC}"
        return 0
    else
        echo -e "${RED}✗ FAILED${NC}"
        echo "Response: $response"
        return 1
    fi
}

echo "1. Проверка базовых сервисов"
echo "----------------------------"
check_service "Eureka Discovery Server" "$EUREKA_URL"
check_service "Config Server" "$CONFIG_URL/user-service/default"
check_service "API Gateway" "$BASE_URL/actuator/health" || echo -e "${YELLOW}⚠ API Gateway health endpoint может быть недоступен${NC}"
echo ""

echo "2. Проверка Config Server"
echo "-------------------------"
check_json "User Service Config" "$CONFIG_URL/user-service/default"
check_json "Auth Service Config" "$CONFIG_URL/auth-service/default"
check_json "Bid Service Config" "$CONFIG_URL/bid-service/default"
check_json "Category Service Config" "$CONFIG_URL/category-service/default"
echo ""

echo "3. Тестирование User Service"
echo "----------------------------"
echo -n "Регистрация пользователя... "
USER_RESPONSE=$(curl -s -X POST "$BASE_URL/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "username": "testuser",
    "password": "password123"
  }')

if [ $? -eq 0 ] && [[ "$USER_RESPONSE" == *"{"* ]]; then
    echo -e "${GREEN}✓ OK${NC}"
    # Попытка извлечь ID (простой способ без jq)
    USER_ID=$(echo "$USER_RESPONSE" | grep -o '"id"[[:space:]]*:[[:space:]]*[0-9]*' | grep -o '[0-9]*' | head -1)
    if [ ! -z "$USER_ID" ]; then
        check_json "Получение пользователя" "$BASE_URL/users/$USER_ID"
    fi
else
    echo -e "${RED}✗ FAILED${NC}"
    echo "Response: $USER_RESPONSE"
fi

check_json "Список пользователей" "$BASE_URL/users?page=0&size=10"
echo ""

echo "4. Тестирование Auth Service"
echo "-----------------------------"
echo -n "Регистрация через Auth Service... "
AUTH_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "auth@example.com",
    "username": "authuser",
    "password": "password123"
  }')

if [ $? -eq 0 ] && [[ "$AUTH_RESPONSE" == *"{"* ]]; then
    echo -e "${GREEN}✓ OK${NC}"
else
    echo -e "${RED}✗ FAILED${NC}"
    echo "Response: $AUTH_RESPONSE"
fi
echo ""

echo "5. Тестирование Category Service (Reactor + R2DBC)"
echo "---------------------------------------------------"
echo -n "Создание категории... "
CATEGORY_RESPONSE=$(curl -s -X POST "$BASE_URL/categories" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Electronics",
    "parent_id": null
  }')

if [ $? -eq 0 ] && [[ "$CATEGORY_RESPONSE" == *"{"* ]]; then
    echo -e "${GREEN}✓ OK${NC}"
    # Попытка извлечь ID
    CATEGORY_ID=$(echo "$CATEGORY_RESPONSE" | grep -o '"id"[[:space:]]*:[[:space:]]*[0-9]*' | grep -o '[0-9]*' | head -1)
    if [ ! -z "$CATEGORY_ID" ]; then
        check_json "Получение категории" "$BASE_URL/categories/$CATEGORY_ID"
        check_json "Дерево категорий" "$BASE_URL/categories/tree"
    fi
else
    echo -e "${RED}✗ FAILED${NC}"
    echo "Response: $CATEGORY_RESPONSE"
fi

check_json "Список категорий" "$BASE_URL/categories?page=0&size=10"
echo ""

echo "6. Тестирование Bid Service (Reactor + R2DBC)"
echo "----------------------------------------------"
check_json "Список ставок" "$BASE_URL/bids/lots/1?page=0&size=10"
echo ""

echo "========================================="
echo "Тестирование завершено"
echo "========================================="
echo ""
echo "Для детальной проверки Circuit Breaker:"
echo "1. Остановите user-service"
echo "2. Попробуйте зарегистрироваться через auth-service"
echo "3. Должен сработать fallback"
echo ""
echo "Для проверки Eureka:"
echo "Откройте в браузере: $EUREKA_URL"
echo ""

