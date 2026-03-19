# CLAUDE.md — LoaderApp (GruzchikiApp)

Этот файл читается Claude Code автоматически. Все правила обязательны. Уровень — senior+/staff.
Никаких костылей, заглушек, TODO-комментариев и временных решений без явного согласования.

---

## Проект

Android-приложение для управления грузчиками. Две роли: **диспетчер** и **грузчик**.
- Package: `com.loaderapp`
- minSdk 24 / targetSdk 34
- Kotlin + Jetpack Compose Material 3 + Hilt + Room + DataStore + Coroutines/Flow
- Три окружения: `dev`, `stage`, `prod` (product flavors)
- Тёмная тема (единственная поддерживаемая)

---

## Модульная структура

```
:app            — точка входа, навигация, DI wiring уровня приложения
:core           — общие контракты и утилиты (AppResult, ошибки, логирование, dispatchers, domain-модели)
:core-ui        — общие Compose компоненты и тема
:feature-orders — вертикальный slice: data / domain / presentation / ui
```

### Разрешённый граф зависимостей

```
:app → :core
:app → :core-ui
:app → :feature-orders
:feature-orders → :core
:feature-orders → :core-ui
:core → (ничего)
:core-ui → :core
```

### Запрещено

- Циклические зависимости между Gradle-модулями
- Зависимость feature-модуля от `:app`
- Импорт `data`-слоя одной фичи в другую фичу напрямую
- Новые фичи внутри `:app` — каждая фича = отдельный модуль `:feature-<name>`

---

## Архитектурные слои (внутри фичи)

Направление зависимостей: `data → domain ← presentation → ui`

### `domain`
- Бизнес-модели, use-cases, repository interfaces
- **Нет** Android-импортов, Room, Compose
- Чистый Kotlin

### `data`
- Реализации repository, DAO, Entity, DB, mapper `Entity ↔ domain-model`
- Зависит от `domain`-контрактов, не наоборот
- Room-миграции обязательны при каждом изменении схемы; schema JSON коммитится в `/app/schemas/`

### `presentation`
- `ViewModel`, `UiState`, `UiEvent`, `UiEffect`
- Маппинг `domain → UiModel` здесь, не в `ui`
- **Нет** Room/DAO/Entity импортов

### `ui`
- Только Compose rendering и локальное визуальное состояние
- **Нет** бизнес-логики, **нет** прямого импорта `data`
- Принимает `UiState`, отправляет `UiEvent`

---

## База данных

**Текущая версия: 6**

| Таблица | Ключевые поля |
|---|---|
| `users` | id, name, phone, role, rating, birthDate, avatarInitials, createdAt |
| `chat_messages` | id, orderId, senderId, senderName, senderRole, text, sentAt |
| `orders` | id, address, dateTime, cargoDescription, pricePerHour, estimatedHours, requiredWorkers, minWorkerRating, status, createdAt, completedAt, workerId, dispatcherId, workerRating, comment |
| `order_workers` | orderId, workerId, takenAt (junction-таблица для мультигрузчиков) |

### Правила миграций

- Каждое изменение схемы = новая пронумерованная `Migration(from, to)` в data-слое фичи
- Schema JSON экспортируется автоматически (`room.schemaLocation`) и **коммитится** в репозиторий
- `fallbackToDestructiveMigration` **запрещён** в production-коде
- Миграционные тесты в `androidTest` обязательны для каждой новой миграции

---

## DI (Hilt)

- Один интерфейс → один binding на конкретную конфигурацию сборки
- Variant-specific замены (fake для `debug`) через source set — не через `if (BuildConfig.DEBUG)` в модуле
- Bindings между фичами только через API-контракты (интерфейсы), без импорта чужих `data`-реализаций
- DI-интеграция между `:app` и фичами — в Hilt-модулях уровня `app/di/features/`
- `@Singleton` только там, где действительно нужен один инстанс на весь процесс

---

## Логирование

Используй `AppLogger` (инжектируется через Hilt) — **никогда** `Log.d/e` напрямую в бизнес-коде.

| Билд | Реализация |
|---|---|
| `debug` | `LogcatAppLogger` |
| `release` | `SentryAppLogger` |
| `NoOpAppLogger` | для тестов |

- `logger.breadcrumb(category, message, data)` — для навигационных/бизнес-событий
- `logger.captureException(throwable, tag, message)` — для обработанных ошибок

---

## Обработка ошибок

- Используй `AppResult<T>` (из `:core`) вместо `try/catch` в ViewModel
- Repository возвращает `AppResult`, ViewModel маппит в `UiState`
- **Никаких** `!!` (non-null assertion) кроме случаев, где null невозможен по контракту типа
- **Никаких** глотаний исключений (`catch (e: Exception) { }`)

---

## Coroutines и Flow

- `viewModelScope` только в ViewModel
- Repository и data source работают через `Flow` или `suspend fun`
- `StateFlow` для UiState, `SharedFlow` для UiEffect (one-shot events)
- Dispatcher инжектируется (`IoDispatcher`, `MainDispatcher`) — не хардкодится
- `withContext(Dispatchers.IO)` в data-слое, не в ViewModel

---

## Compose

- Все `@Composable` функции — stateless, состояние поднято в ViewModel или caller
- `UiState` как единственный источник правды для экрана
- Анимации через стандартные Compose API (`AnimatedVisibility`, `animateContentSize`, etc.)
- Кастомные компоненты — в `:core-ui`, не дублируются между фичами
- Тема: бирюзовая цветовая схема, только тёмная тема (`LoaderAppTheme`)
- `HorizontalPager` + `TabRow` синхронизированы для свайп-навигации между вкладками

---

## Кодстайл

- Kotlin Coding Conventions + Android Kotlin Style Guide
- Форматирование: `ktlint` (конфиг в `.editorconfig`)
- Один класс/интерфейс — один файл; имя файла = имя класса
- Конструктор `@Inject` на отдельной строке (см. существующий код)
- `data class` для UiState и domain-моделей
- `sealed interface` для UiEvent и UiEffect
- Явные типы возврата у публичных функций

---

## Окружения

| Flavor | applicationId suffix | BASE_URL | Логирование |
|---|---|---|---|
| `dev` | `.dev` | `api-dev.loaderapp.local` | verbose |
| `stage` | `.stage` | `api-stage.loaderapp.local` | verbose |
| `prod` | — | `api.loaderapp.local` | нет |

- `BuildConfig`-поля читаются только через `AppConfig`/`AppBuildInfo` интерфейсы, не напрямую в фичах
- Sentry инициализируется вручную (`auto-init=false`), только в `release` билде, только если `sentryDsn` не пустой

---

## Запрещено всегда

- `TODO`, `FIXME`, `HACK` комментарии без issue-ссылки
- Временные решения без явного обсуждения
- `Thread.sleep()`, `runBlocking` в production-коде (допустимо только в тестах)
- Хардкод строк, цветов, размеров вне ресурсов/темы
- `android.util.Log` напрямую в бизнес-логике
- `!!" (non-null assertion) там, где можно обойтись без него
- Неиспользуемые импорты и переменные
- `@SuppressWarnings`/`@Suppress` без объяснения в комментарии рядом

---

## Git

### Стратегия веток

- `master` — стабильная, только после зелёной локальной проверки (см. ниже)
- `feature/<n>` — каждая задача в отдельной ветке, создаётся от `master`
- `fix/<n>` — аналогично для багфиксов
- После локальной проверки: merge в `master` напрямую, затем `git push origin master`

### Workflow для каждой задачи

```bash
# 1. Создать ветку
git checkout master && git pull origin master
git checkout -b feature/<task-name>

# 2. Делать изменения, коммитить
git add -A
git commit -m "[Module] Short description"

# 3. Прогнать локальную проверку (см. секцию ниже)
# 4. Если всё зелёное — merge и push
git checkout master
git merge --no-ff feature/<task-name>
git push origin master
```

### Коммит-сообщения

Формат: `[Module] Short description`
Примеры: `[feature-orders] Add worker rating flow`, `[core] Extract AppResult`, `[app] Wire OrdersFeature DI`

Правила:
- Каждый коммит компилируемый
- Schema JSON коммитится **в том же коммите** что и миграция
- Не коммитить `build/`, `.gradle/`, `local.properties`

---

## Локальная проверка перед пушем (эквивалент CI)

CI запускает: статический анализ → unit-тесты → сборка APK. Локально — те же команды.

```bash
# Всё одной командой — полный эквивалент CI
./gradlew detektAll ktlintCheckAll testDebugUnitTest :app:assembleDevDebug :app:assembleProdRelease --stacktrace
```

Или по шагам:

```bash
# 1. Статический анализ + форматирование
./gradlew detektAll ktlintCheckAll --stacktrace

# 2. Unit-тесты
./gradlew testDebugUnitTest --stacktrace

# 3. Сборка smoke
./gradlew :app:assembleDevDebug :app:assembleProdRelease --stacktrace
```

**Claude Code обязан прогонять эту проверку после каждого значимого изменения** и показывать результат. Не предлагать коммит если есть ошибки сборки, падающие тесты или ошибки ktlint/detekt.

### Автоисправление форматирования

```bash
./gradlew ktlintFormatAll
```

Запускать перед `ktlintCheckAll` при форматных ошибках.

### Отчёты после прогона

- `build/reports/tests/` — unit-тесты
- `build/reports/detekt/` — статический анализ
- `build/reports/ktlint/` — форматирование

### Установка на эмулятор

```bash
# Установить devDebug на подключённый эмулятор/устройство
./gradlew :app:installDevDebug
```

Или через Android Studio: Build Variants → `devDebug` → Run.
