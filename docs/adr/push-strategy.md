# ADR-push-strategy

## Status
Proposed

## Context
Mobile push notifications are required for order lifecycle and chat signals, while preserving offline-first behavior and server-authoritative domain consistency.

## Decision
- Use FCM as the push transport at architecture/design level (no SDK/runtime integration in this change).
- Keep state server-authoritative: push informs clients to refetch, not to trust payload as final state.
- Use minimal payload with identifiers and routing metadata only.
- Use direct device tokens (no topic broadcast).
- Apply `notificationId`-based deduplication.
- Apply `collapseKey` per entity stream.
- Treat client `acknowledgeHandled(notificationId)` as an optional business-handling signal (analytics/diagnostics), not transport-level delivery receipt.

## Consequences
### Positive
- Stronger privacy: no PII and no rich business payload in notifications.
- Improved consistency: clients converge via refetch against canonical backend state.
- Safer retries: deduplication key prevents duplicated user-facing effects.
- Better personalization and access control through direct token targeting.

### Negative
- Higher backend complexity for token management and recipient resolution.
- Additional client/server roundtrip after push to materialize actual state.
- More observability requirements to detect token churn and delivery degradation.

## Alternatives considered
- Topics: rejected due to weaker personalization and harder authorization boundaries.
- Rich payload with embedded state: rejected due to staleness/PII risks and higher schema coupling.
- Polling-only model: rejected due to worse latency, battery/network inefficiency, and poorer UX for transactional events.
