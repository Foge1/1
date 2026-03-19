# Code quality checks

## Локальный запуск

```bash
./gradlew detektAll ktlintCheckAll
```

- `detektAll` запускает статический анализ Kotlin-кода во всех модулях.
- `ktlintCheckAll` проверяет форматирование и базовые style-правила.

## Автоисправление форматирования

```bash
./gradlew ktlintFormatAll
```

После автоформатирования рекомендуется повторно выполнить `./gradlew ktlintCheckAll`.

## Что делать при падении CI

1. Открыть лог шага **Lint Check** в PR.
2. Если упал `ktlintCheckAll` — выполнить `./gradlew ktlintFormatAll`, проверить diff и закоммитить изменения.
3. Если упал `detektAll` — исправить выявленную проблему в коде (ошибки потенциальных багов, сложность, корутины, неиспользуемый код).
4. Повторно запустить локально `./gradlew detektAll ktlintCheckAll` перед push.

## Принципы конфигурации

- Конфигурация линтеров централизована в Gradle convention plugin `loaderapp.lint` (included build `build-logic`).
- Правила настроены так, чтобы исключить шум и ложные падения на legacy-коде.
- Для Kotlin Android/JVM модулей линтеры подключаются через `id "loaderapp.lint"` в секции `plugins`.

## Detekt quality gates

Detekt работает в режиме строгих quality gates для production-кода и проверяет группы правил:

- complexity
- potential-bugs
- coroutines
- exceptions
- performance
- style

Важно:

- baseline не используется;
- `ignoreFailures` не используется;
- массовые `@Suppress` не используются как стратегия прохождения проверок;
- нарушения detekt исправляются в коде, а не скрываются suppress-ами.

Formatting-правила detekt не используются: форматирование полностью остаётся за `ktlint`.

## Архитектурные guardrails (Detekt ForbiddenImport)

Чтобы предотвращать архитектурные регрессы без ложных срабатываний, включены точечные правила `ForbiddenImport`.

### Какие guardrails действуют

1. **UI не импортирует data/persistence слоя app-модуля**
   - Запрещены импорты `com.loaderapp.data..*` и `com.loaderapp.di.data..*`.
   - Область действия: `com.loaderapp.ui..*` (`app/src/main/java/com/loaderapp/ui/**`).
   - Цель: UI работает через presentation/domain API, а не через DAO, entities, репозитории и DI data-модули.

2. **Feature-модули не импортируют app internals**
   - Запрещены импорты `com.loaderapp.di..*`, а также `com.loaderapp.LoaderApplication` и `com.loaderapp.MainActivity`.
   - Область действия: все feature-модули (`feature-*/src/main/java/**`).
   - Цель: фичи остаются изолированными, не зависят от app wiring и не ломают модульные границы.

### Как правильно строить зависимости

- `ui` → зависит от `presentation` моделей/состояний/команд.
- `presentation` → зависит от `domain` use-case/контрактов.
- `domain` → по возможности не зависит от Android SDK и деталей хранения.
- `data`/`di` wiring → остаются в app или data-слое; наружу отдают только интерфейсы/контракты.
- Feature-модули интегрируются через публичные контракты (core/domain API), а не через `app` классы.
