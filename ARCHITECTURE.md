# GruzchikiApp — Архитектура проекта

## Стек технологий

| Слой | Технология |
|------|-----------|
| UI | Jetpack Compose + Material3 |
| Навигация | Navigation Compose |
| ViewModel | Hilt ViewModel + StateFlow |
| DI | Dagger Hilt |
| База данных | Room (SQLite) |
| Хранилище настроек | DataStore Preferences |
| Архитектурный паттерн | Clean Architecture (Data / Domain / Presentation) |
| Язык | Kotlin + Coroutines/Flow |
| Min SDK | 24 (Android 7.0) |

---

## Структура пакетов

```
com.loaderapp/
│
├── core/                          ← Общие утилиты, не привязанные к фичам
│   └── common/
│       ├── Result.kt              ← Sealed class для результата операций
│       └── UiState.kt             ← Sealed class для состояния UI
│
├── data/                          ← Data Layer (Room, DataStore, Mapper-ы)
│   ├── AppDatabase.kt             ← Room database (version 5, schema-export enabled)
│   ├── Converters.kt              ← TypeConverters для Room
│   ├── dao/                       ← DAO-интерфейсы Room
│   │   ├── OrderDao.kt
│   │   ├── UserDao.kt
│   │   ├── OrderWorkerDao.kt
│   │   └── ChatDao.kt
│   ├── datasource/local/          ← Локальные DataSource (обёртки над DAO)
│   ├── mapper/                    ← Конверторы Entity ↔ Domain Model
│   ├── model/                     ← Room Entity классы (@Entity)
│   ├── preferences/               ← DataStore (UserPreferences)
│   └── repository/                ← Реализации domain/repository интерфейсов
│       ├── OrderRepositoryImpl.kt
│       ├── UserRepositoryImpl.kt
│       └── ChatRepositoryImpl.kt
│       ⚠️ AppRepository.kt УДАЛЁН (был legacy God-object, заменён отдельными репо)
│
├── domain/                        ← Domain Layer (чистая бизнес-логика)
│   ├── model/                     ← Domain-модели (без зависимостей от фреймворка)
│   │   ├── OrderModel.kt
│   │   ├── UserModel.kt
│   │   └── ChatMessageModel.kt
│   ├── repository/                ← Интерфейсы репозиториев
│   │   ├── OrderRepository.kt
│   │   ├── UserRepository.kt
│   │   └── ChatRepository.kt
│   └── usecase/                   ← Use Cases (один класс — одна операция)
│       ├── base/UseCase.kt
│       ├── order/                 ← CreateOrder, TakeOrder, CompleteOrder, Cancel...
│       └── user/                  ← GetLoaders, GetUserById, CreateUser
│
├── di/                            ← Hilt DI модули
│   ├── AppModule.kt               ← UserPreferences, Dispatchers
│   ├── DatabaseModule.kt          ← Room DB, DAO providers
│   └── RepositoryModule.kt        ← Binds интерфейс → реализацию
│
├── features/                      ← 🆕 Изолированные фичи (будущие модули)
│   ├── auth/                      ← Аутентификация (Login, Register, Session)
│   │   ├── domain/
│   │   │   ├── repository/AuthRepository.kt   ← TODO: impl
│   │   │   └── usecase/LoginUseCase.kt         ← TODO: impl
│   │   └── presentation/AuthViewModel.kt       ← TODO: экран Login/Register
│   │
│   ├── payments/                  ← Оплата заказов
│   │   ├── domain/
│   │   │   ├── model/PaymentModel.kt
│   │   │   ├── repository/PaymentRepository.kt ← TODO: impl
│   │   │   └── usecase/CreatePaymentUseCase.kt ← TODO: impl
│   │   └── presentation/          ← TODO: PaymentsScreen, PaymentViewModel
│   │
│   ├── chat/                      ← Чат внутри заказа (real-time)
│   │   ├── domain/
│   │   │   ├── repository/ChatFeatureRepository.kt ← TODO: impl (WebSocket/Firebase)
│   │   │   └── usecase/SendMessageUseCase.kt        ← TODO: impl
│   │   └── presentation/          ← TODO: ChatScreen, ChatViewModel
│   │
│   └── ratings/                   ← Рейтинги и отзывы
│       ├── domain/
│       │   ├── repository/RatingRepository.kt ← TODO: impl
│       │   └── usecase/           ← TODO: RateWorkerUseCase, GetRatingHistoryUseCase
│       └── presentation/          ← TODO: RatingScreen, RatingViewModel
│
├── navigation/                    ← Навигационный граф
│   ├── AppNavGraph.kt
│   └── Route.kt                   ← Sealed class маршрутов
│
├── notification/                  ← Уведомления
│   └── NotificationHelper.kt
│
├── presentation/                  ← ViewModels для текущих экранов
│   ├── base/BaseViewModel.kt
│   ├── loader/LoaderViewModel.kt
│   └── dispatcher/DispatcherViewModel.kt
│
└── ui/                            ← Compose экраны
    ├── auth/RoleSelectionScreen.kt
    ├── components/                ← Переиспользуемые компоненты
    ├── dispatcher/                ← DispatcherScreen, CreateOrderScreen, Dialog
    ├── history/HistoryScreen.kt
    ├── loader/LoaderScreen.kt
    ├── order/OrderDetailScreen.kt
    ├── profile/ProfileScreen.kt
    ├── rating/RatingScreen.kt
    ├── settings/SettingsScreen.kt
    ├── splash/SplashScreen.kt
    └── theme/                     ← Color, Type, Shape, Theme
```

---

## Storage/Room decision

В проекте осознанно используются **2 отдельные Room-базы**:

1. `loader_app_database` (`com.loaderapp.data.AppDatabase`, version 5) — legacy-данные приложения: пользователи, чат, базовые заказы и связи.
2. `orders_feature_database` (`com.loaderapp.features.orders.data.local.db.OrdersDatabase`, version 3) — изолированный storage новой Orders-фичи (orders/applications/assignments + собственные миграции).

Почему это допустимо в Фазе 1:
- Границы ответственности не пересекаются напрямую по таблицам, что снижает риск регрессий при эволюции новой Orders-фичи.
- Можно независимо версионировать и тестировать миграции в feature-DB.
- Полный merge storage-слоёв отложен: это отдельная продуктовая/архитектурная задача, не входящая в стабилизационную фазу.

Риск, закрытый в этой фазе: destructive migration не используется, схема экспортируется в `app/schemas`, миграции регистрируются явно.

---

## Что было изменено

### Удалено / Перемещено
- **`AppRepository.kt`** — удалён. Это был God-object, агрегировавший все DAO напрямую. Заменён тремя чистыми реализациями: `OrderRepositoryImpl`, `UserRepositoryImpl`, `ChatRepositoryImpl`.

### Добавлено
- **`features/auth/`** — каркас для авторизации по телефону + PIN
- **`features/payments/`** — каркас для модуля оплаты заказов
- **`features/chat/`** — каркас для real-time чата (WebSocket / Firebase)
- **`features/ratings/`** — каркас для рейтинговой системы
- **`app/src/test/`** — структура для unit-тестов с заглушками (CreateOrderUseCaseTest, UserRepositoryImplTest)
- **`ARCHITECTURE.md`** — этот документ

---

## Следующие шаги (план разработки)

1. **Auth**: Реализовать `AuthRepositoryImpl` (LocalDB → позже Server JWT)
2. **Payments**: Подключить платёжный шлюз → реализовать `PaymentRepositoryImpl`
3. **Chat (real-time)**: Firebase Realtime DB или WebSocket → `ChatFeatureRepositoryImpl`
4. **Ratings**: Агрегация рейтингов по завершённым заказам → `RatingRepositoryImpl`
5. **Тесты**: Подключить `mockk` + `turbine` и заполнить стабы в `src/test/`
6. **Миграции БД**: При обновлении схемы Room — добавлять миграции вместо `fallbackToDestructiveMigration`

---

## Правила архитектуры

- **Domain layer** не знает ни о Room, ни о Hilt, ни об Android SDK
- **UseCase** = один публичный метод, одна бизнес-операция
- **ViewModel** не имеет прямого доступа к DAO — только через UseCase
- **Новые фичи** добавлять в `features/<name>/` со своими domain/data/presentation подпапками
- **Общие компоненты** (без привязки к фиче) — в `ui/components/`
