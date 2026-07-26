# serve-rest-automation-tests

[![Run API Tests](https://github.com/therealyourvanechka/serve-rest-automation-tests/actions/workflows/ci.yml/badge.svg)](https://github.com/therealyourvanechka/serve-rest-automation-tests/actions/workflows/ci.yml)

Allure-отчёт: [https://therealyourvanechka.github.io/serve-rest-automation-tests](https://therealyourvanechka.github.io/serve-rest-automation-tests)

## Содержание

- [О проекте](#о-проекте)
- [Стек](#стек)
- [Архитектура](#архитектура)
- [Покрытие тестами](#покрытие-тестами)
- [Техники тест-дизайна](#техники-тест-дизайна)
- [CI/CD](#cicd)
- [Локальный запуск](#локальный-запуск)

## О проекте
Фреймворк автотестов на Java для **ServeRest API** — учебного e-commerce API с авторизацией, товарами и корзиной

Задача была не просто покрыть CRUD, а разобраться в реальной бизнес-логике и осознанно применить техники тест-дизайна

Работа началась с подробного [**чеклиста проверок**](serverest-checklist.md), где были применены техники тест-дизайна:

- **таблица принятия решений** для четырёх комбинаций токенов — применяется на всех защищённых эндпоинтах (Usuarios, Produtos, Carrinhos)
- **граничные значения** для остатка товара
- **диаграмма переходов состояний** для корзины: два состояния (активна/не активна), семь переходов

Отдельно реализованы **E2E-сценарии**. Самый интересный — тест на **оверселлинг**: система не должна позволить продать то, чего физически нет (два пользователя пытаются одновременно купить один и тот же товар с остатком 1)

В коде: клиентский слой с отдельными клиентами на каждый EP, Builder-паттерн и фабрика для тестовых данных, контроллеры с тестами, Allure для отчётов, GitHub Actions для CI

В результате покрыты все четыре EP, включая бизнес-правила


## Стек

| Инструмент | Версия | Назначение |
|---|---|---|
| Java | 21 | Язык разработки |
| Gradle | 9.0 | Сборщик проекта |
| REST Assured | 5.5.0 | HTTP-клиент |
| JUnit 5 | 5.11.0 | Фреймворк тестирования |
| AssertJ | 3.26.3 | Fluent-утверждения |
| Allure | 2.29.1 | Отчётность |
| Lombok | 1.18.46 | Boilerplate-код |
| DataFaker | 2.4.3 | Генерация тестовых данных |
| JJWT | 0.12.6 | Генерация JWT |

## Архитектура

Проект построен по слоистой архитектуре:

```
src/
├── main/java/com/serverest/
│   ├── client/          # HTTP-клиенты (Auth, Usuarios, Produtos, Carrinhos)
│   ├── model/           # DTO (request/response)
│   ├── exception/       # Исключения
│   └── util/            # Specifications (REST Assured)
│
└── test/java/com/serverest/
    ├── BaseTest.java    # Базовый класс тестов
    ├── controllers/     # Тестовые классы по эндпоинтам
    └── util/            # ServeRestDataFactory, JwtHelper
```

- **Client-слой**: каждый эндпоинт представлен классом с типизированными методами для позитивных сценариев и raw-методами для негативных
- **Controller-слой**: содержит только проверки
- **DTO**: Jackson-аннотации, Lombok
- **DataFactory**: возвращает builder для гибкой кастомизации в тестах

## Покрытие тестами

57 тестов — все проходят.

| Раздел | Тестов | Что проверяем                                                        |
|---|---|----------------------------------------------------------------------|
| Login | 4 | Успешный/неуспешный вход, несуществующий email                       |
| Usuarios | 11 | CRUD + фильтры                                                       |
| Produtos | 25 | CRUD + граничные значения цены и количества + админ-доступ + фильтры |
| Carrinhos | 15 | CRUD + граничные значения количества + состояния                     |
| E2E | 2 | overselling + жизненный цикл                                         |


## Техники тест-дизайна

| Техника | Где используется                                       |
|---|--------------------------------------------------------|
| Таблица принятия решений | Авторизация для POST/PUT/DELETE /produtos              |
| Классы эквивалентности | Поля моделей (цена, количество, email, имя)            |
| Анализ граничных значений | Количество товаров в корзине)                          |
| Таблица переходов состояний | S0 (нет корзины) -> S1 (активна), 7 переходов |


## CI/CD

GitHub Actions

**Стадии**
1) build: сборка проекта
2) test: запуск тестов, генерация Allure-отчёта, деплой на GitHub Pages

**Триггеры** 
- push
- pull_request в `main`
- workflow_dispatch

После каждого успешного прогона Allure-отчёт публикуется на GitHub Pages
**Allure-отчёт:** [https://therealyourvanechka.github.io/serve-rest-automation-tests](https://therealyourvanechka.github.io/serve-rest-automation-tests)

## Локальный запуск

**Требования:** Java 21+

```bash
./gradlew test
```

Для Allure-отчёта локально:

```bash
./gradlew test allureReport
open build/allureReport/index.html
```
