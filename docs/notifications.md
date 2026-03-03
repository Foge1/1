# Notifications & Push Design

## 1. Goals
- Reliable near-real-time domain notifications.
- No PII in payload.
- Server-authoritative state.
- Safe deduplication and retry.
- Compatible with offline-first model.

## 2. Event taxonomy

### new_order_published
- **Trigger:** Dispatcher publishes a new order and it becomes visible to eligible loaders.
- **Recipients:** Eligible loaders in coverage area / capability scope.
- **Preconditions:** Order status transitions to `PUBLISHED` and is not soft-deleted.
- **Delivery priority:** High.
- **Android channel id:** `orders_updates`.
- **Deep link:** `loaderapp://orders/{orderId}`.
- **Collapse policy:** Collapse by order entity (`collapseKey = order_{orderId}`).
- **ShouldReplaceExisting:** `true`.

### application_submitted
- **Trigger:** Loader submits an application for an order.
- **Recipients:** Dispatcher (order owner).
- **Preconditions:** Order status is `PUBLISHED`; loader is eligible and not already selected.
- **Delivery priority:** High.
- **Android channel id:** `orders_updates`.
- **Deep link:** `loaderapp://orders/{orderId}/responses`.
- **Collapse policy:** Collapse by order responses stream (`collapseKey = order_responses_{orderId}`).
- **ShouldReplaceExisting:** `true`.

### loader_selected
- **Trigger:** Dispatcher selects a loader for an order.
- **Recipients:** Selected loader and dispatcher.
- **Preconditions:** Order status is `PUBLISHED`; application exists and is valid.
- **Delivery priority:** High.
- **Android channel id:** `orders_updates`.
- **Deep link:** `loaderapp://orders/{orderId}`.
- **Collapse policy:** Collapse by order entity (`collapseKey = order_{orderId}`).
- **ShouldReplaceExisting:** `true`.

### order_started
- **Trigger:** Selected loader starts execution of the order.
- **Recipients:** Dispatcher and selected loader.
- **Preconditions:** Order status is `ASSIGNED`; actor is authorized participant.
- **Delivery priority:** High.
- **Android channel id:** `orders_updates`.
- **Deep link:** `loaderapp://orders/{orderId}`.
- **Collapse policy:** Collapse by order entity (`collapseKey = order_{orderId}`).
- **ShouldReplaceExisting:** `true`.

### order_completed
- **Trigger:** Order completion is confirmed and persisted server-side.
- **Recipients:** Dispatcher and selected loader.
- **Preconditions:** Order status is `IN_PROGRESS`; completion validation passes.
- **Delivery priority:** High.
- **Android channel id:** `orders_updates`.
- **Deep link:** `loaderapp://orders/{orderId}`.
- **Collapse policy:** Collapse by order entity (`collapseKey = order_{orderId}`).
- **ShouldReplaceExisting:** `true`.

### order_cancelled
- **Trigger:** Dispatcher cancels order or cancellation is enforced by platform policy.
- **Recipients:** Dispatcher, selected loader, and active applicants (if still relevant).
- **Preconditions:** Order status is not terminal (`COMPLETED`, `CANCELLED`); cancellation reason accepted.
- **Delivery priority:** High.
- **Android channel id:** `orders_updates`.
- **Deep link:** `loaderapp://orders/{orderId}`.
- **Collapse policy:** Collapse by order entity (`collapseKey = order_{orderId}`).
- **ShouldReplaceExisting:** `true`.

### new_chat_message
- **Trigger:** New message persisted in order chat.
- **Recipients:** Chat participants except the sender.
- **Preconditions:** Participant has access to order chat and message visibility is allowed.
- **Delivery priority:** High.
- **Android channel id:** `chat_messages`.
- **Deep link:** `loaderapp://orders/{orderId}/chat`.
- **Collapse policy:** Collapse by conversation (`collapseKey = chat_{orderId}`).
- **ShouldReplaceExisting:** `false`.

### reminder_upcoming_order
- **Trigger:** Scheduler detects upcoming order start window.
- **Recipients:** Dispatcher and selected loader.
- **Preconditions:** Order has scheduled start time within configured reminder horizon and is non-terminal.
- **Delivery priority:** Normal.
- **Android channel id:** `reminders`.
- **Deep link:** `loaderapp://orders/{orderId}`.
- **Collapse policy:** Collapse by reminder window (`collapseKey = reminder_{orderId}`).
- **ShouldReplaceExisting:** `true`.

## 3. Payload contract

Example payload:

```json
{
  "eventType": "order_started",
  "orderId": "ord_123",
  "version": 8,
  "notificationId": "notif_abc123",
  "collapseKey": "order_ord_123",
  "deepLink": "loaderapp://orders/ord_123",
  "timestamp": "2026-01-25T10:20:00Z"
}
```

Rules:
- No personal data in payload (no names, phone numbers, addresses, chat text).
- No chat content in payload for `new_chat_message`; only identifiers and metadata.
- Client performs authoritative refetch after receiving a push.
- `version` is used to detect and ignore stale push events.

## 4. Android Channels matrix

| Channel ID | Name | Importance | Used for | Group |
|-------------|------|------------|----------|-------|
| `orders_updates` | Order updates | `IMPORTANCE_HIGH` | Order lifecycle transitions (`new_order_published`, `loader_selected`, `order_started`, `order_completed`, `order_cancelled`, `application_submitted`) | `orders` |
| `chat_messages` | Chat messages | `IMPORTANCE_HIGH` | `new_chat_message` events requiring quick participant reaction | `communication` |
| `reminders` | Order reminders | `IMPORTANCE_DEFAULT` | `reminder_upcoming_order` soft reminders | `orders` |

Importance rationale:
- `IMPORTANCE_HIGH` is used where user attention directly impacts transactional flow (order assignment, start, completion, chat response latency).
- `IMPORTANCE_DEFAULT` is used for reminders to avoid unnecessary interruption while preserving visibility.

## 5. Deduplication & collapse strategy

- `notificationId` is the globally unique event identifier.
- `collapseKey` groups multiple updates for the same entity stream (order or chat).
- If a push arrives with an already processed `notificationId`, client ignores it as duplicate.
- If `collapseKey` matches an active notification, client updates existing notification content/state instead of creating a new stack item.


### Acknowledgement policy

- `acknowledgeHandled(notificationId)` is used for:
  - analytics
  - deduplication
  - server-side delivery diagnostics
- It does not affect order business status.
- The system remains correct even without acknowledgement because idempotency is guaranteed by `notificationId`.

## 6. Delivery model

- Direct device tokens are used (no topic fanout).
- Why:
  - Events are personalized and authorization-scoped per user/order access.
  - Security posture is stronger with server-side recipient resolution.
  - Access control remains explicit and auditable per notification dispatch.

## 7. Token lifecycle

- Register device token on login/session start.
- Refresh token binding on token rotation.
- Unregister token on logout/session revoke.
- Perform server-side cleanup for invalid tokens reported by provider.
- Support multiple active tokens per account (multi-device).

## 8. Failure handling

- Duplicate delivery is tolerated by idempotent processing.
- Expired/stale `version` events are ignored.
- Missing local entity triggers silent refetch (no blocking error to user).
- Permanent delivery failures are logged and surfaced to server observability pipelines.
