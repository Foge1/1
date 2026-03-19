# ADR-0005: Feature boundaries via domain API contracts

## Status
Accepted

## Context
При ревью зависимостей выявлены два риска нарушения границ фич:
1. Orders-фича получала данные о текущем пользователе через `UserRepository` + DataStore ключ `current_user_id`, что создаёт скрытую связность с деталями auth/session persistence.
2. Общий `di/data/DatabaseModule` создавал Room storage Orders-фичи (`OrdersDatabase`, `OrdersDao`, `ApplicationsDao`, `AssignmentsDao`), что размывало ownership feature persistence.

Для масштабирования фич нужна жёсткая граница: потребитель не зависит от data-слоя владельца.

## Decision
1. Ввести feature API-контракт `AuthSessionApi` в `features.auth.domain.api`.
   - Методы: `observeCurrentUser(): Flow<User?>`, `getCurrentUserOrNull(): User?`.
   - Контракт описан на domain-моделях auth-фичи (`features.auth.domain.model.User`), без Room/DAO.
2. Реализацию контракта оставить во владельце (auth): `AuthRepositoryImpl : AuthRepository, AuthSessionApi`.
3. Потребитель (orders) зависит только от `AuthSessionApi`.
   - `CurrentUserProviderImpl` больше не использует `UserRepository` и DataStore напрямую.
4. Hilt wiring:
   - bind `AuthSessionApi <- AuthRepositoryImpl` в `FeatureRepositoryModule`.
5. Перенести Orders Room provisioning в feature-область:
   - новый `di/features/orders/OrdersDataModule` создаёт `OrdersDatabase` и её DAO.
   - `di/data/DatabaseModule` оставляет только legacy `AppDatabase` и его DAO.

## Import rules
- Запрещено: импортировать `features.<other_feature>.data.*` из другой фичи.
- Запрещено: использовать `@Entity/@Dao/mapper` другой фичи вне её data-слоя.
- Разрешено: зависеть от `features.<feature>.domain.api.*` (или `feature-api`) + domain модели контракта.

## Ownership rules
- `Entity/Dao/Migration` принадлежат feature-owner и не покидают его data boundary.
- Межфичевое взаимодействие — только через API-интерфейсы владельца.
- Hilt modules с persistence-провайдерами должны быть расположены в скоупе соответствующей фичи.

## Example
- **До**: Orders -> `UserRepository` + DataStore(`current_user_id`) для определения текущего пользователя.
- **После**: Orders -> `AuthSessionApi`; Auth владеет сессией и трансляцией пользователя.

## Consequences
- Явные точки интеграции между фичами, меньше скрытых связей.
- Проще выделять фичи в отдельные Gradle-модули (API остаётся стабильным).
- Снижается риск утечки persistence-моделей в чужие слои.
