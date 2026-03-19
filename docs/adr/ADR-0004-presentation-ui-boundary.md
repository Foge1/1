# ADR-0004: Presentation/UI boundary hardening for Orders screens

## Status
Accepted

## Context
Phase 2 migration left an architectural tail: part of screen logic stayed in Compose UI layer.
In particular, history search/filter/grouping and section-title derivation were performed in UI
components, which mixed rendering with orchestration concerns.

## Decision
1. Fix the boundary:
   - `presentation` owns `ViewModel`, `UiState`, `UiEvent/UiEffect`, and mappers `domain -> ui`.
   - `ui` owns only rendering and routing user events upward.
2. Move history transformation logic from `ui/components/HistoryScreen.kt` to
   `features/orders/presentation/OrdersViewModel.kt` (`buildHistoryState`).
3. Change `HistoryScreen` contract to accept ready `DispatcherHistoryUiState` instead of raw list.
4. Move Orders presentation artifacts from `features/orders/ui` package to
   `features/orders/presentation` package.

## Consequences
- Compose layer became declarative-only for orders history flow.
- Mapping/orchestration is centralized in ViewModel and easier to unit-test.
- Package naming now matches Clean boundaries (`presentation` vs `ui`).

## Anti-patterns (explicitly forbidden)
- `domain/entity -> ui` mapping in composables.
- Domain filtering/grouping logic inside UI widgets.
- UI imports from `data/*` to access mapper/business objects.
