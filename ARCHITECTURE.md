# LoaderApp — архитектура (актуальное состояние)

## Gradle modules

| Module | Тип | Назначение | Прямые project-зависимости |
|---|---|---|---|
| `:app` | application | Точка входа, навигация, интеграция фич, DI wiring уровня приложения | `:core`, `:feature-orders` |
| `:core` | library | Общие контракты и утилиты (`AppResult`, ошибки, логирование, dispatchers, общие domain-модели) | — |
| `:feature-orders` | library | Вертикальная Orders-фича: `data + domain + presentation + ui` в одном модуле | `:core` |

Источник истины по составу модулей: `settings.gradle`.

---

## Правила зависимостей модулей

### Разрешённый граф

- `:app -> :core`
- `:app -> :feature-orders`
- `:feature-orders -> :core`
- `:core` не зависит от `:app` и feature-модулей.

### Запрещено

- Циклические зависимости между Gradle-модулями.
- Зависимость feature-модуля от `:app`.
- Доступ к чужому data-слою через прямой module dependency (при появлении новых feature-модулей).

---

## Границы слоёв (domain / data / presentation / ui)

Принцип направления зависимостей внутри фичи: `data -> domain <- presentation -> ui`.

### `domain`
- Бизнес-модели, правила, use-case логика, repository interfaces.
- Не содержит Android/Room/Compose деталей.

### `data`
- Реализации repository, DAO/Entity/DB, data source, mapper между persistence и domain.
- Может зависеть от `domain` контрактов.

### `presentation`
- `ViewModel`, `UiState/UiEvent/UiEffect`, orchestration и mapper `domain -> ui`.
- Не содержит Room/DAO/Entity.

### `ui`
- Compose rendering и локальное визуальное состояние.
- Не выполняет бизнес-решения и не импортирует `data` напрямую.

---

## Границы фич

### Базовые правила

1. Фича владеет своим `data/domain/presentation/ui` и своей persistence-моделью.
2. Межфичевое взаимодействие — только через API/интерфейсы (контракты уровня domain/api).
3. Импорты `data` одной фичи в другую фичу запрещены.
4. `@Entity/@Dao/migrations` не выходят за границы feature-owner.

### Практика в текущем коде

- Orders-фича получает пользователя через контракт `AuthSessionApi` (bridge), а не через прямой доступ к auth/data.
- DI-интеграция между приложением и фичей выполняется в Hilt-модулях уровня интеграции (`app/di/features/*`).

---

## DI правила

1. На один интерфейс в конкретном варианте сборки (`debug`/`release`) должен приходиться один binding.
2. Variant-specific замены (например, fake-реализация в `debug`) оформляются через source set variant-а и не ломают контракт интерфейса.
3. Binding’и между фичами оформляются через API-контракты, без импорта чужих data-реализаций.

---

## Масштабирование модульности

Текущая стратегия: **одна фича = один Gradle-модуль** (vertical slice с собственными слоями). При выделении новых фич применяется тот же шаблон:

- новый модуль `:feature-<name>`;
- API-контракты для межфичевого взаимодействия;
- отсутствие прямых `data`-импортов между фичами.
