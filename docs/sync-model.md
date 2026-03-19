# Sync Model (Server-authoritative, PR 3.6)

## 1) Goals / Non-goals

### Goals
- Define deterministic synchronization behavior between mobile clients and backend.
- Prevent lost updates via optimistic concurrency.
- Ensure safe retries for create/mutation operations via idempotency.
- Support offline-first client delivery through outbox-compatible contract.

### Non-goals
- No transport implementation details (Retrofit/OkHttp/websocket setup).
- No client storage schema migrations in this document.
- No conflict-free replicated data type (CRDT) protocol.

## 2) Versioning model

- Each mutable entity has:
  - `version: Int` (monotonic per entity, incremented by server on each successful mutation).
  - `updatedAt: ISO-8601 UTC` (server clock).
- **Primary concurrency mechanism (chosen):** `Version` header sent by client for mutation requests.
  - Example: `Version: 7`
  - If request version != current entity version, server responds `409 Conflict` with unified error body.
- Read endpoints always return latest `version` and `updatedAt`.
- Optional alternative (`ETag`/`If-Match`) is acknowledged but not primary in this contour.

## 3) Idempotency

- `Idempotency-Key` header is required for:
  - create endpoints (`POST /orders`, `POST /orders/{id}/applications`, `POST /orders/{id}/chat/messages`)
  - transition endpoint (`POST /orders/{id}/transitions`)
  - selection endpoint (`POST /orders/{id}/applications/{applicationId}/select`)
- Server stores idempotency records for limited TTL (e.g., 24h; exact value configurable).
- Repeated request with same key + same semantic payload returns the same status code and body as the original successful/failed processed result.
- Reuse of same key with different payload returns validation/conflict error (`409` or `422` by server policy).

## 4) Conflict resolution

- **Authoritative side:** server state wins.
- On `409 Conflict` client strategy:
  1. Refetch entity (`GET /orders/{id}` or related resource).
  2. Re-evaluate intent against new state and lifecycle rules.
  3. Retry with fresh `Version` only if business action is still valid.
- Minimal merge policy:
  - Merge-friendly (append-only): chat messages by unique message id/order timeline.
  - Non-merge (authoritative overwrite with retry flow): order status, selected loader, application selection decisions.
- Client must not auto-merge status transitions locally without server confirmation.

## 5) Offline-first / Outbox

Recommended local outbox schema for future mobile client implementation:

| Field | Type | Meaning |
|---|---|---|
| `operationId` | string/uuid | Local unique operation identifier |
| `entityType` | enum/string | `order`, `application`, `chat_message`, etc. |
| `entityId` | string | Target entity id (or temporary id for creates) |
| `operationType` | enum/string | `create_order`, `transition`, `send_message`, ... |
| `payload` | json/blob | Serialized request body + headers subset |
| `createdAt` | timestamp | Local creation time |
| `attempts` | int | Number of retry attempts |
| `lastError` | string? | Last failure description/code |

Operational rules:
- Retry policy: exponential backoff with jitter (e.g., base 1s, cap 5m).
- Deduplication key: `operationId` + `Idempotency-Key`.
- Ordering guarantee: FIFO **per entity** to preserve transition order (e.g., do not send `complete` before `start_in_progress`).
- Permanent failures (`403`, semantic `422`) move operation to dead-letter/manual resolution queue.
