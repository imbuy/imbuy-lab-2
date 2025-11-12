# ImBuy Microservices Lab 2

Микросервисная архитектура для аукционной платформы ImBuy.

## Архитектура

- **discovery-server** (Eureka) - Сервис регистрации и обнаружения
- **config-server** - Централизованный сервер конфигурации
- **api-gateway** - API Gateway для маршрутизации запросов
- **user-service** - Сервис управления пользователями (Spring Data JPA)
- **auth-service** - Сервис аутентификации с Circuit Breaker (Feign Client)
- **category-service** - Сервис категорий (Reactor + Spring Data R2DBC)
- **lot-service** - Сервис лотов (Spring Data JPA)
- **bid-service** - Сервис ставок (Reactor + R2DBC)

```bash
docker-compose up -d
```
```bash
./start-all.sh
```
```bash
# Запуск тестов
./test-services.sh
```
### Eureka Dashboard
http://localhost:8761

### Config Server
http://localhost:8888/{service-name}/default

### API Gateway
http://localhost:8080

```bash
./stop-all.sh
```
