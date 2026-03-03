# ADR-sync-conflicts

## Status
Proposed

## Context
Order and application flows are mutable and role-dependent. Concurrent updates from multiple clients can produce stale writes and race conditions. We need a clear conflict strategy compatible with offline-first clients.

## Decision
- Use optimistic concurrency with server-controlled integer `version`.
- Client must send current `Version` on mutation requests.
- Server returns `409 Conflict` on mismatch.
- Conflict handling strategy: refetch authoritative state, re-evaluate business intent, retry only if still valid.
- Merge only append-safe domains (chat timeline); do not auto-merge order status/assignment changes.

## Consequences
### Positive
- Prevents silent lost updates.
- Keeps conflict behavior deterministic and easy to reason about.
- Compatible with outbox/retry mobile strategy.

### Negative
- Additional client complexity for 409 handling and refetch.
- Increased request volume under high contention.

## Alternatives considered
- **Last-write-wins by timestamp**: rejected due to potential business rule violations.
- **Pessimistic locking**: rejected due to scalability and UX issues for mobile intermittent connectivity.
