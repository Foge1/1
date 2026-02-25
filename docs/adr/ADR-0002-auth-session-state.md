# ADR-0002: Auth SessionState as single source of truth

## Status
Accepted

## Context
LoaderApp had split session logic between UI-level `SessionViewModel` and `UserPreferences`, while auth feature was still TODO.
This caused implicit coupling and made it hard to reason about transitions (restore/login/logout/error).

## Decision
1. Introduce auth-domain models:
   - `User` (auth projection of user profile).
   - `SessionState` sealed interface: `Unauthenticated`, `Authenticating`, `Authenticated(user)`, `Error(error)`.
2. Make `AuthRepository.observeSession(): Flow<SessionState>` the **single source of truth** for session lifecycle.
3. `AuthRepositoryImpl` stores/clears `current_user_id` in DataStore and derives session state from that key + user lookup.
4. `SessionViewModel` and `AuthViewModel` subscribe to repository session flow and map it to screen/navigation state.

## Consequences
- Session transitions are explicit and deterministic across app startup/login/logout.
- Navigation can react declaratively to one stream of truth.
- Error handling uses `AppResult/AppError` instead of throw-based control flow for regular outcomes.
