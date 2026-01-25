# Микросервис контроля месячных лимитов расходов

## Описание

Тестовое задание Junior Java Developer  
Цель — прототип микросервиса для учёта расходных операций в разных валютах с контролем месячных лимитов в USD

## Основной функционал

- Приём расходных транзакций
- Автоматический пересчёт суммы в USD по курсу на день операции
- Определение превышения месячного лимита
- Установка нового месячного лимита по категории
- Получение списка всех установленных лимитов
- Получение списка транзакций, превысивших актуальный на момент операции лимит
- Хранение истории лимитов
- Хранение курсов валют в собственной таблице
- Использование кэшированных курсов, запрос к внешнему API только при необходимости

## Стек технологий

- Язык: Java 21
- Фреймворк: Spring Boot 4
- База данных: PostgreSQL
- Миграции: Flyway 
- Маппинг: MapStruct
- Lombok 
- Документация API: SpringDoc OpenAPI
- Тестирование: JUnit 5, Mockito
- Maven

## API Endpoints

1. Транзакции  (/api/transactions)
- POST / - Принять новую расходную операцию
- GET /exceeded - Получить список транзакций, превысивших лимит
2. Лимиты (/api/limits)
- POST / - Установить новый месячный лимит
- GET / - Получить все установленные лимиты

## Настройка и запуск

### Требования

- Java 21+
- PostgreSQL 14+
- Maven 3.8+

### Шаги запуска

1. Настройка базы данных
 ```
   CREATE DATABASE testtask_db;
   CREATE USER testtask_user WITH PASSWORD 'password';
   GRANT ALL PRIVILEGES ON DATABASE testtask_db TO testtask_user;
 ```
2. Настройка переменных окружения

   Создайте файл application-local.yml в src/main/resources/:
```
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/testtask_db
    username: your_username
    password: your_password
    driver-class-name: org.postgresql.Driver

alphavantage:
  api-key: your_api_key
```

3. Получение API ключа Alpha Vantage

- Перейдите на alphavantage.co и получите ключ
- Полученный ключ укажите в конфигурации

4. Сборка и запуск
```
mvn clean package
mvn spring-boot:run
```

5. Проверка работы

- Приложение доступно по адресу: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/v3/api-docs

## Тестирование

Проект включает два типа тестов:

### Unit-тесты

- Тестирование бизнес-логики TransactionService
- Мокирование зависимостей (репозитории, внешние сервисы)
- Проверка различных сценариев превышения лимитов

### Интеграционные тесты

- Тестирование REST контроллеров
- Использование текущей БД
- Проверка полного потока данных

### Запуск тестов
```
mvn test
```

## Миграции базы данных

Используется Flyway для управления схемой БД:

- Миграции находятся в src/main/resources/db/migration/
- Автоматическое применение при запуске приложения

Основная миграция (V1__init_schema.sql):

- Создание таблиц exchange_rates, limits, transactions
- Индексы для оптимизации запросов

## Примеры использования

### Создание лимита

Метод: POST

Путь: http://localhost:8080/api/limits
```
{
    "category": "PRODUCT",
    "limitSum": 1500.00
}
```

### Создание транзакции

Метод: POST

Путь: http://localhost:8080/api/transactions
```
{
    "accountFrom": "0000000123",
    "accountTo": "9999999999",
    "currencyShortname": "KZT",
    "sum": 350000,
    "expenseCategory": "PRODUCT",
    "datetime": "2026-01-25T12:10:00+03:00"
}
```

### Список транзакций, превысивших лимит

Метод: GET

Путь: http://localhost:8080/api/transactions/exceeded

## Список лимитов

Метод: GET

Путь: http://localhost:8080/api/limits

## Обработка ошибок

Сервис возвращает структурированные ошибки:

- 400 - Ошибки валидации входных данных
- 404 - Ресурс не найден
- 500 - Внутренние ошибки сервера