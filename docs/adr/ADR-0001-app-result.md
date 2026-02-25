# ADR-0001: Unified AppResult and AppError

## Context
В проекте одновременно использовались `Result` с текстовыми сообщениями и исключения как control-flow, что усложняло обработку ошибок между data/domain/presentation.

## Decision
Вводим `AppResult<T>` с двумя исходами: `Success<T>` и `Failure(error: AppError)`.
`AppError` описывает типизированные категории ошибок (Network/Backend/Auth/Validation/NotFound/Storage/Unknown).
Добавляем `appRunCatching`, `Throwable.toAppError()` и Flow-утилиты `asResult`/`mapResult`.
Старый `Result` оставляем как deprecated-совместимость для пошаговой миграции, плюс адаптеры в обе стороны.

## Consequences
Новый код должен возвращать `AppResult` и не использовать исключения как нормальный путь выполнения.
Presentation может единообразно маппить `AppError` в UI-состояния и сообщения.
