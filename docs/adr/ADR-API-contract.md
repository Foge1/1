# ADR-API-contract

## Status
Proposed

## Context
Mobile and backend teams need a shared, implementation-agnostic contract for Auth, Orders, Applications, Chat and Sync behavior. Current app logic should not be modified in this iteration, but server contour decisions must be explicit enough for parallel backend/client development.

## Decision
- Introduce a markdown API contract (`docs/api-contract.md`) with OpenAPI-like structure.
- Define unified headers, pagination, filtering/sorting, and error model.
- Freeze initial endpoint set for Auth, Orders, Applications, Chat.
- Define explicit order lifecycle transitions and role-based access constraints.
- Define chat participation and status-gated access rules.

## Consequences
### Positive
- Backend can implement endpoints with predictable request/response envelopes.
- Future API client implementation can start from stable contracts.
- Reduced ambiguity in authorization, transitions and common errors.

### Negative
- Contract-first approach may require revisions once production constraints appear.
- Markdown is less machine-verifiable than strict OpenAPI yaml/json.

## Alternatives considered
- **Delay contract until backend prototype**: rejected due to cross-team blocking risk.
- **Create full OpenAPI spec immediately**: deferred to keep scope focused on architectural alignment first.
