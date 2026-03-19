# Room schema export и migration tests

## Где хранятся схемы
- `app/schemas`
- `feature-orders/schemas`

Схемы являются частью репозитория и коммитятся в Git вместе с изменениями миграций/структуры базы.

## Как генерируются схемы
Для Kotlin-модулей используется только `kapt` с аргументом `room.schemaLocation`:

- `app/build.gradle`:
  - `kapt { arguments { arg("room.schemaLocation", "$projectDir/schemas") } }`
- `feature-orders/build.gradle`:
  - `kapt { arguments { arg("room.schemaLocation", file("$projectDir/schemas").path) } }`

Канон:
- не дублировать `schemaLocation` во втором месте (например, через `javaCompileOptions.annotationProcessorOptions`);
- не создавать каталоги из Gradle configuration phase (`mkdirs()` и подобные side-effects запрещены).

## Migration tests и assets
Чтобы `MigrationTestHelper` видел экспортированные схемы, `androidTest` assets должны включать каталог `schemas` в каждом модуле, где есть миграционные тесты:

- `androidTest.assets.srcDirs += files("$projectDir/schemas")` (или эквивалент с `file(...)`).

## Базовые команды для проверки
- Unit tests:
  - `./gradlew :app:testDebugUnitTest`
  - `./gradlew :feature-orders:testDebugUnitTest`
- Code quality:
  - `./gradlew detektAll ktlintCheckAll`
- Проверка сборки:
  - `./gradlew :app:assembleDebug :app:assembleRelease :feature-orders:assembleDebug :feature-orders:assembleRelease`
