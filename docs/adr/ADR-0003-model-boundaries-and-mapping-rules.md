# ADR-0003: Model boundaries and mapping rules for Orders

## Status
Accepted

## Context
LoaderApp has two coexisting orders contexts:
- legacy orders flow (`domain.model.OrderModel` + legacy Room entities);
- new feature orders flow (`features.orders.domain.Order` + feature Room entities).

Over time, mapping helpers leaked into UI package (`features/orders/ui`), which increased coupling between
UI and domain variants and made architecture boundaries blurry.

## Decision
1. Keep **feature orders domain** (`features.orders.domain.*`) as owner of current business lifecycle
   (applications, selection, assignments, start/complete/cancel rules).
2. Treat legacy `domain.model.OrderModel` as compatibility/read-model for legacy screens until full migration.
3. Separate model categories explicitly:
   - **Domain model**: business behavior/contracts.
   - **Persistence model**: Room `@Entity` and DB projections.
   - **UI model/state**: render-oriented structures and ephemeral flags.
4. Place cross-context mappers in **data layer** (`features/orders/data/mappers/*`), not UI.
5. Enforce boundary rule: repositories/use-cases expose domain models only; entities are internal to data.

## Consequences
- Lower coupling: UI no longer imports legacy order domain model through feature UI package.
- Clear migration path: legacy read-model can be phased out screen-by-screen without touching Room contracts.
- Easier review/testing: mapping policy is centralized and explicit.

## Anti-patterns
- Returning Room entities from repository interfaces.
- Mapping domain/persistence types inside composables or ui-state files.
- Duplicating same business model in multiple packages «for convenience».
