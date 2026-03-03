# API Contract (Server Contour, PR 3.6)

## 1) Overview

### Domains
- **Auth** — login/refresh/logout flows and session lifecycle.
- **Users** — user profile summary used in orders/applications/chat contexts.
- **Orders** — create/read/list orders, status transitions.
- **Applications (Responses)** — loaders respond to orders, dispatcher selects performer.
- **Chat** — order-scoped messaging between order participants.
- **Sync** — optimistic concurrency, cursor pagination, idempotency behavior.

### Roles
- **dispatcher** — creates/manages orders, selects loader, controls status transitions from dispatcher side.
- **loader** — browses/accepts work via applications, performs operational transitions (start/complete), participates in chat after selection.

## 2) Common

### Base URL
- Placeholder: `https://{api-host}/v1`

### Auth scheme
- **Access token**: Bearer JWT in `Authorization` header.
- **Refresh token**: opaque or JWT token in refresh endpoint body/cookie (contract uses body).

### Headers
- `Authorization: Bearer <jwt>` — required for protected endpoints.
- `Idempotency-Key: <uuid>` — required for create/mutation endpoints (`POST /orders`, transitions, applications, chat send).
- `Version: <int>` — optimistic concurrency header (primary contract choice).
- `X-Request-Id: <string>` — optional request tracing/correlation.

### Pagination
- Cursor model:
  - Request: `?cursor=<opaque>&limit=<1..100>`
  - Response:
    ```json
    {
      "items": [],
      "limit": 20,
      "nextCursor": "opaque-next-cursor-or-null"
    }
    ```

### Filtering/sorting
- Query params (where relevant):
  - `status`
  - `updatedFrom` / `updatedTo` (ISO-8601 UTC)
  - `sort` (e.g. `updatedAt:desc`, `createdAt:asc`)

### Unified error model
```json
{
  "code": "ORDER_VERSION_CONFLICT",
  "message": "Order version mismatch",
  "details": {
    "expectedVersion": 6,
    "actualVersion": 7
  },
  "traceId": "req-2f3a..."
}
```

- `code`: machine-readable string.
- `message`: human-readable string.
- `details`: optional object.
- `traceId`: optional tracing id.

## 3) Entities (minimal, no PII)

> For all entities: `id`, `version` (int), `updatedAt` (ISO-8601 UTC).

### UserSummary
```json
{
  "id": "usr_123",
  "version": 3,
  "updatedAt": "2026-01-25T10:12:11Z",
  "role": "loader",
  "rating": 4.8,
  "isOnline": true
}
```

### Order
```json
{
  "id": "ord_456",
  "version": 7,
  "updatedAt": "2026-01-25T10:13:41Z",
  "status": "assigned",
  "createdBy": "usr_dispatcher_1",
  "selectedLoaderId": "usr_loader_9",
  "cargoType": "boxes",
  "scheduledAt": "2026-01-25T13:00:00Z"
}
```

### Application (Response)
```json
{
  "id": "app_1001",
  "version": 1,
  "updatedAt": "2026-01-25T10:15:00Z",
  "orderId": "ord_456",
  "loaderId": "usr_loader_9",
  "status": "submitted"
}
```

### ChatMessage
```json
{
  "id": "msg_301",
  "version": 1,
  "updatedAt": "2026-01-25T10:17:00Z",
  "orderId": "ord_456",
  "senderId": "usr_loader_9",
  "text": "Arriving in 15 minutes"
}
```

## 4) Endpoints list

### Auth
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

### Orders
- `GET /orders`
- `POST /orders`
- `GET /orders/{orderId}`
- `POST /orders/{orderId}/transitions`

### Applications
- `POST /orders/{orderId}/applications`
- `GET /orders/{orderId}/applications`
- `POST /orders/{orderId}/applications/{applicationId}/select`

### Chat
- `GET /orders/{orderId}/chat/messages`
- `POST /orders/{orderId}/chat/messages`

## 5) Endpoint details

---
### POST /auth/login
- **Purpose**: Authenticate user and issue token pair.
- **Role access**: public.
- **Request**
  - Headers: `X-Request-Id` (optional)
  - Body example:
    ```json
    {
      "login": "dispatcher_01",
      "password": "***"
    }
    ```
- **Response**
  ```json
  {
    "accessToken": "jwt-access",
    "refreshToken": "refresh-token",
    "expiresInSeconds": 3600,
    "user": {
      "id": "usr_dispatcher_1",
      "version": 2,
      "updatedAt": "2026-01-25T10:00:00Z",
      "role": "dispatcher",
      "rating": null,
      "isOnline": true
    }
  }
  ```
- **Errors**: `401`, `403`, `422`.

---
### POST /auth/refresh
- **Purpose**: Rotate access token using refresh token.
- **Role access**: authenticated session holder.
- **Request**
  - Headers: `X-Request-Id` (optional)
  - Body example:
    ```json
    {
      "refreshToken": "refresh-token"
    }
    ```
- **Response**
  ```json
  {
    "accessToken": "new-jwt-access",
    "refreshToken": "new-refresh-token",
    "expiresInSeconds": 3600
  }
  ```
- **Errors**: `401`, `403`, `422`.

---
### POST /auth/logout
- **Purpose**: Invalidate refresh token/session.
- **Role access**: dispatcher, loader.
- **Request**
  - Headers: `Authorization`, `X-Request-Id` (optional)
  - Body example:
    ```json
    {
      "refreshToken": "refresh-token"
    }
    ```
- **Response**
  ```json
  {
    "success": true
  }
  ```
- **Errors**: `401`, `403`, `422`.

---
### GET /orders
- **Purpose**: List orders available to current role with filters/pagination.
- **Role access**: dispatcher, loader.
- **Request**
  - Headers: `Authorization`, `X-Request-Id` (optional)
  - Query example:
    - `?status=assigned&updatedFrom=2026-01-01T00:00:00Z&sort=updatedAt:desc&cursor=abc&limit=20`
- **Response**
  ```json
  {
    "items": [
      {
        "id": "ord_456",
        "version": 7,
        "updatedAt": "2026-01-25T10:13:41Z",
        "status": "assigned",
        "createdBy": "usr_dispatcher_1",
        "selectedLoaderId": "usr_loader_9",
        "cargoType": "boxes",
        "scheduledAt": "2026-01-25T13:00:00Z"
      }
    ],
    "limit": 20,
    "nextCursor": "next-xyz"
  }
  ```
- **Errors**: `401`, `403`, `422`.

---
### POST /orders
- **Purpose**: Create a new order.
- **Role access**: dispatcher.
- **Request**
  - Headers: `Authorization`, `Idempotency-Key`, `X-Request-Id` (optional)
  - Body example:
    ```json
    {
      "cargoType": "boxes",
      "scheduledAt": "2026-01-25T13:00:00Z",
      "notes": "fragile"
    }
    ```
- **Response**
  ```json
  {
    "id": "ord_789",
    "version": 1,
    "updatedAt": "2026-01-25T10:11:00Z",
    "status": "created",
    "createdBy": "usr_dispatcher_1",
    "selectedLoaderId": null,
    "cargoType": "boxes",
    "scheduledAt": "2026-01-25T13:00:00Z"
  }
  ```
- **Errors**: `401`, `403`, `409`, `422`.

---
### GET /orders/{orderId}
- **Purpose**: Get order details by id.
- **Role access**: dispatcher (owner), loader (if allowed by visibility policy).
- **Request**
  - Headers: `Authorization`, `X-Request-Id` (optional)
- **Response**
  ```json
  {
    "id": "ord_456",
    "version": 7,
    "updatedAt": "2026-01-25T10:13:41Z",
    "status": "assigned",
    "createdBy": "usr_dispatcher_1",
    "selectedLoaderId": "usr_loader_9",
    "cargoType": "boxes",
    "scheduledAt": "2026-01-25T13:00:00Z"
  }
  ```
- **Errors**: `401`, `403`, `404`.

---
### POST /orders/{orderId}/transitions
- **Purpose**: Perform order status transition.
- **Role access**: dispatcher, loader (role depends on transition type).
- **Request**
  - Headers: `Authorization`, `Idempotency-Key`, `Version`, `X-Request-Id` (optional)
  - Body example:
    ```json
    {
      "transition": "start_in_progress",
      "reason": null
    }
    ```
- **Response**
  ```json
  {
    "id": "ord_456",
    "version": 8,
    "updatedAt": "2026-01-25T10:20:00Z",
    "status": "in_progress",
    "createdBy": "usr_dispatcher_1",
    "selectedLoaderId": "usr_loader_9",
    "cargoType": "boxes",
    "scheduledAt": "2026-01-25T13:00:00Z"
  }
  ```
- **Errors**: `401`, `403`, `404`, `409`, `422`.

---
### POST /orders/{orderId}/applications
- **Purpose**: Submit loader application for order.
- **Role access**: loader.
- **Request**
  - Headers: `Authorization`, `Idempotency-Key`, `X-Request-Id` (optional)
  - Body example:
    ```json
    {
      "comment": "Ready to start now"
    }
    ```
- **Response**
  ```json
  {
    "id": "app_1001",
    "version": 1,
    "updatedAt": "2026-01-25T10:15:00Z",
    "orderId": "ord_456",
    "loaderId": "usr_loader_9",
    "status": "submitted"
  }
  ```
- **Errors**: `401`, `403`, `404`, `409`, `422`.

---
### GET /orders/{orderId}/applications
- **Purpose**: Get applications for an order.
- **Role access**: dispatcher (owner), loader (only own application as filtered view).
- **Request**
  - Headers: `Authorization`, `X-Request-Id` (optional)
  - Query example: `?cursor=abc&limit=20&sort=updatedAt:desc`
- **Response**
  ```json
  {
    "items": [
      {
        "id": "app_1001",
        "version": 1,
        "updatedAt": "2026-01-25T10:15:00Z",
        "orderId": "ord_456",
        "loaderId": "usr_loader_9",
        "status": "submitted"
      }
    ],
    "limit": 20,
    "nextCursor": null
  }
  ```
- **Errors**: `401`, `403`, `404`, `422`.

---
### POST /orders/{orderId}/applications/{applicationId}/select
- **Purpose**: Select loader application, assign order.
- **Role access**: dispatcher.
- **Request**
  - Headers: `Authorization`, `Idempotency-Key`, `Version`, `X-Request-Id` (optional)
  - Body example:
    ```json
    {
      "note": "Selected by rating"
    }
    ```
- **Response**
  ```json
  {
    "id": "ord_456",
    "version": 8,
    "updatedAt": "2026-01-25T10:18:00Z",
    "status": "assigned",
    "createdBy": "usr_dispatcher_1",
    "selectedLoaderId": "usr_loader_9",
    "cargoType": "boxes",
    "scheduledAt": "2026-01-25T13:00:00Z"
  }
  ```
- **Errors**: `401`, `403`, `404`, `409`, `422`.

---
### GET /orders/{orderId}/chat/messages
- **Purpose**: Fetch order-scoped chat messages.
- **Role access**: dispatcher + selected loader (participants only).
- **Request**
  - Headers: `Authorization`, `X-Request-Id` (optional)
  - Query example: `?cursor=abc&limit=50&updatedFrom=2026-01-25T10:00:00Z&sort=updatedAt:asc`
- **Response**
  ```json
  {
    "items": [
      {
        "id": "msg_301",
        "version": 1,
        "updatedAt": "2026-01-25T10:17:00Z",
        "orderId": "ord_456",
        "senderId": "usr_loader_9",
        "text": "Arriving in 15 minutes"
      }
    ],
    "limit": 50,
    "nextCursor": "next-msg"
  }
  ```
- **Errors**: `401`, `403`, `404`, `422`.

---
### POST /orders/{orderId}/chat/messages
- **Purpose**: Send chat message in order context.
- **Role access**: dispatcher + selected loader (participants only).
- **Request**
  - Headers: `Authorization`, `Idempotency-Key`, `Version`, `X-Request-Id` (optional)
  - Body example:
    ```json
    {
      "text": "Please confirm ETA"
    }
    ```
- **Response**
  ```json
  {
    "id": "msg_302",
    "version": 1,
    "updatedAt": "2026-01-25T10:18:00Z",
    "orderId": "ord_456",
    "senderId": "usr_dispatcher_1",
    "text": "Please confirm ETA"
  }
  ```
- **Errors**: `401`, `403`, `404`, `409`, `422`.

## 6) Orders lifecycle

| Status | Allowed transitions | Initiator role |
|---|---|---|
| `created` | `publish`, `cancel` | dispatcher |
| `published` | `assign_loader`, `cancel` | dispatcher |
| `assigned` | `start_in_progress`, `cancel` | loader (start), dispatcher (cancel) |
| `in_progress` | `complete`, `cancel` | loader (complete), dispatcher (cancel by policy) |
| `completed` | _(none, terminal)_ | — |
| `cancelled` | _(none, terminal)_ | — |

### Invariants
- `cancel` allowed only from: `created`, `published`, `assigned`, `in_progress`.
- `completed` is terminal; no further transitions.
- `cancelled` is terminal; no further transitions.
- `assign_loader` allowed only if order has at least one `submitted` application.
- `start_in_progress` allowed only for the selected loader.
- `complete` allowed only after `in_progress`.

## 7) Chat access rules

- Chat is strictly **order-scoped**.
- Participants only:
  - dispatcher who owns the order;
  - loader selected for that order.
- Status-gated access:
  - read/write: `assigned`, `in_progress`;
  - read-only: `completed`;
  - denied: before assignment and after cancellation.
- Access outside order context is forbidden (`403` or `404` depending on disclosure policy).
