# Tech debt policy and registry

## Почему TODO без трекинга запрещены

Комментарии `TODO`/`FIXME` без контекста со временем перестают быть actionable: непонятно, кто владелец, зачем задача нужна и как понять, что она завершена. Это приводит к скрытому риску в production-коде, деградации качества и «заметанию под ковёр» потенциальных дефектов.

Поэтому в проекте запрещены «голые» TODO/FIXME в runtime-коде.

## Формат допустимого TODO

Допустимы только следующие форматы:

- `TODO(#<id>): <кратко что/почему/критерий готовности>`
- `TODO(TECH-DEBT-XXX): <кратко что/почему/критерий готовности>`

Правила:

1. В комментарии обязательно должны быть:
   - **что** нужно сделать,
   - **почему** это не сделано сейчас,
   - **критерий завершения** (definition of done).
2. Для `TODO(TECH-DEBT-XXX)` обязательна запись в таблице ниже.
3. TODO не должен маскировать баг или небезопасное поведение. Если есть риск падения/повреждения данных — сначала делается минимальный safety-fix.

## Где хранится список долга

Единый реестр технического долга ведётся в этом документе (`docs/tech-debt.md`) в таблице **Tech debt registry**.

## Как закрывать долг

1. Сделать PR, который закрывает конкретный пункт из таблицы по его ID.
2. В описании PR указать ссылку на ID (например, `TECH-DEBT-004`).
3. Удалить/обновить соответствующий TODO в коде.
4. Удалить строку из реестра или отметить как закрытую (если нужен audit trail).

## Tech debt registry

| ID | Location | Description | Risk | Plan / Exit criteria |
|----|----------|-------------|------|----------------------|
| TECH-DEBT-001 | `app/src/main/java/com/loaderapp/features/payments/domain/usecase/CreatePaymentUseCase.kt` | Подключить платёжный шлюз и вынести создание платежа в интеграционный сценарий с подтверждением статуса провайдера. | Средний: платежи остаются на уровне доменной модели без внешней верификации. | `create/confirm/refund` выполняются через реальный gateway, добавлены интеграционные тесты потоков статусов. |
| TECH-DEBT-002 | `app/src/main/java/com/loaderapp/features/payments/domain/repository/PaymentRepository.kt` | Реализовать `PaymentRepositoryImpl` с идемпотентностью и обработкой ошибок провайдера. | Средний: контракт есть, но production-реализация платежей отсутствует. | Реализация подключена в DI, покрыта e2e-сценариями оплаты/возврата, используется в runtime path. |
| TECH-DEBT-003 | `app/src/main/java/com/loaderapp/features/payments/domain/model/PaymentModel.kt` | Добавить Room entity + мапперы для локального кеширования платежей. | Низкий/средний: нет локального кеша истории платежей. | Платежи сохраняются в БД, синхронизируются с backend source of truth, есть миграции и тест мапперов. |
| TECH-DEBT-004 | `app/src/main/java/com/loaderapp/features/chat/domain/usecase/SendMessageUseCase.kt` | Переключить отправку на real-time delivery, подтверждаемую стримом событий. | Средний: отправка зависит от текущей реализации репозитория без event-ack. | Use case подтверждает доставку через stream событий, покрыты online/offline сценарии. |
| TECH-DEBT-005 | `app/src/main/java/com/loaderapp/features/chat/domain/repository/ChatFeatureRepository.kt` | Добавить real-time сообщения (WebSocket/Firebase), reconnection policy и ack. | Средний: нет гарантированной семантики доставки сообщений. | Репозиторий реализует reconnect + delivery semantics, подтверждено интеграционными тестами. |
| TECH-DEBT-006 | `app/src/main/java/com/loaderapp/features/ratings/domain/repository/RatingRepository.kt` | Реализовать агрегацию рейтингов по завершённым заказам и консистентную историю. | Средний: рейтинг может быть неполным/неконсистентным до полной реализации. | `getWorkerRating` и `getWorkerRatingHistory` читают согласованные данные из единого источника. |
| TECH-DEBT-007 | `app/src/test/java/com/loaderapp/data/repository/UserRepositoryImplTest.kt` | Добавить in-memory Room для изолированных unit/integration тестов `UserRepositoryImpl`. | Низкий (test-only): текущие тесты отключены и не проверяют репозиторий. | **status: done**. Добавлена in-memory test БД, тесты проверяют `createUser/getUserById`, снят `@Ignore`. PR: текущий. |
| TECH-DEBT-008 | `app/src/test/java/com/loaderapp/domain/usecase/CreateOrderUseCaseTest.kt` | Включить полноценные тесты `CreateOrderUseCase` с mockk/turbine для success/error сценариев. | Низкий (test-only): кейсы описаны, но временно отключены. | **status: done**. Добавлены проверяемые assertions и верификация вызовов репозитория, снят `@Ignore`. PR: текущий. |
| TECH-DEBT-009 | `feature-orders/src/main/java/com/loaderapp/features/orders/data/LegacyOrderRepositoryAdapter.kt` | Точечно исключён из detekt `TooManyFunctions`, т.к. это compatibility shim, вынужденно повторяющий широкий legacy-контракт `OrderRepository`. | Низкий/средний: раздутая поверхность адаптера усложняет поддержку и ревью. | Сузить/пересобрать legacy-контракт `OrderRepository`, мигрировать потребителей на feature-ориентированные контракты и удалить shim + detekt-исключение. |
