# ADR-0006: Gradle Wrapper policy (wrapper JAR in repo, CI без curl)

## Status
Accepted

## Context
Надёжность CI и воспроизводимость сборок зависят от доступности Gradle Wrapper. Если `gradle-wrapper.jar` отсутствует в репозитории и скачивается «на лету», pipeline становится зависимым от внешней сети и shell-скриптов bootstrap.

## Decision
1. Хранить `gradle/wrapper/gradle-wrapper.jar` в репозитории вместе с `gradle-wrapper.properties`.
2. Запуск сборки в CI выполнять через `./gradlew` без дополнительного `curl/wget` для загрузки wrapper-компонентов.
3. Обновление версии Gradle проводить стандартной командой wrapper и коммитить:
   - `gradlew`, `gradlew.bat`
   - `gradle/wrapper/gradle-wrapper.jar`
   - `gradle/wrapper/gradle-wrapper.properties`

## Consequences
- Сборка становится более детерминированной в ограниченных/air-gapped окружениях.
- Уменьшается число точек отказа в CI bootstrap.
- При апдейте Gradle обязательна проверка совместимости Android Gradle Plugin и Kotlin.
