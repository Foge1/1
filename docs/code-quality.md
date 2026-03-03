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
