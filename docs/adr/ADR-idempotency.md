# ADR-idempotency

## Status
Proposed

## Context
Mobile clients may retry requests due to network instability, app restarts, or outbox replay. Without idempotency guarantees, create/transition operations can produce duplicates or inconsistent state.

## Decision
- Require `Idempotency-Key` for create and state-transition endpoints.
- Server persists key records for bounded TTL.
- Repeated requests with same key and same semantic payload must return original processed result.
- Same key with different payload is treated as conflict/validation error.
- Combine idempotency with outbox `operationId` for reliable deduplication.

## Consequences
### Positive
- Safe automatic retries in offline-first clients.
- Reduces duplicate orders/messages/transitions.
- Simplifies at-least-once delivery handling.

### Negative
- Additional server storage and lookup cost.
- Need careful payload fingerprinting policy for "same request" detection.

## Alternatives considered
- **No idempotency, rely on client-side dedupe only**: rejected as insufficient under retries across sessions/devices.
- **Exactly-once delivery infrastructure**: rejected as over-engineering for current stage.
