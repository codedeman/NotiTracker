# Network layer — developer guide

This document explains how the NotiTracker networking stack is structured, how to call it from features, how to add new HTTP endpoints, and how to test code that depends on it.

---

## 1. Goals and boundaries

| Principle | What it means here |
|-----------|-------------------|
| **Single executor** | All HTTP calls go through `NetworkClient.execute(ApiRequest<T>)`. |
| **Typed requests** | Each call is described by an `ApiRequest` (method, path, query, headers, body, response `Type` for Gson). No raw URL strings scattered in repositories. |
| **No OkHttp/Retrofit in UI** | Only classes under `data.remote` (and OkHttp-backed `OkHttpNetworkClient`) depend on OkHttp. Repositories and ViewModels do not import `okhttp3` or `retrofit2`. |
| **Two result types** | **Transport:** `NetworkResult` (`Success` / `Failure` with `AppError`). **UI-oriented:** `NetworkResponse` adds `Loading` and stringly `Error` for existing screens. |

---

## 2. Layering (who calls whom)

```text
Presentation (Activity / Service / ViewModel)
        ↓
Repository interface  ← inject this in features
        ↓
Repository implementation
        ↓
RemoteDataSource interface
        ↓
RemoteDataSource implementation
        ↓
NetworkClient.execute(apiRequest)
        ↓
OkHttpNetworkClient  (OkHttp + Gson only here)
```

**Composition root:** `NetworkGraph` builds `OkHttpClient`, `Gson`, `OkHttpNetworkClient`, `NotificationApi`, datasources, and exposes repositories (e.g. `notificationRepository`).  
**Application:** `NotiTrackerApp` creates the graph once in `onCreate()`:

```kotlin
(application as NotiTrackerApp).networkGraph.notificationRepository
```

When you adopt Hilt (or another DI framework), move the same wiring from `NetworkGraph` into `@Module` / `@Provides` and keep the interfaces unchanged.

---

## 3. Core types (quick reference)

### 3.1 `ApiRequest<T>`

Defined in `data.remote.network.ApiRequest`. Describes one HTTP operation and its decoded body type `T`.

| Property | Role |
|----------|------|
| `method` | `GET`, `POST`, `PUT`, `DELETE` (`HttpMethod`). |
| `path` | Path relative to the **base URL** in `NetworkGraph` (e.g. `"notifications"`, `"v1/chat/completions"`). Resolved with `HttpUrl.resolve`. |
| `query` | Optional query parameters (`Map<String, String?>`). |
| `headers` | Extra headers for this call only. |
| `body` | `okhttp3.RequestBody` for methods that need a body (usually JSON from Gson). |
| `responseType` | `java.lang.reflect.Type` used by Gson to deserialize the response body. |

Concrete request types are typically **`private` classes** inside an `*Api` class (see `NotificationApi`) so the rest of the app only sees factory methods like `getNotifications()` or `postChatCompletion(...)`.

### 3.2 `NetworkClient`

```kotlin
suspend fun <T : Any> execute(request: ApiRequest<T>): NetworkResult<T>
```

Suspend; cancellation follows the coroutine (the client cancels the OkHttp `Call` when the job completes).

### 3.3 `NetworkResult` vs `NetworkResponse`

- **`NetworkResult<T>`** — `Success(data)` or `Failure(error: AppError)`. Used inside **data layer** (datasource / repository mapping).
- **`NetworkResponse<T>`** — adds **`Loading`** and **`Error(message, code?)`**. Some features (e.g. Flow collectors) still use this for UX.

Mapping helper: `NetworkResult.toNetworkResponse()` in `data.remote.network.NetworkResultMapping`.

### 3.4 `AppError`

Centralized failure reasons (`Http`, `Network`, `Serialization`, `Unauthorized`, `Unknown`). Use `error.toUserMessage()` for default user-facing strings when mapping to `NetworkResponse.Error`.

---

## 4. How to use the layer from a feature

### 4.1 Prefer the repository

Inject or obtain **`NotificationRepository`** (interface), not `NetworkClient` directly.

- **One-shot / suspend:** e.g. `getSummary(...)` returns `NetworkResponse<SummaryResponse>`.
- **Stream:** e.g. `getNotifications(): Flow<NetworkResponse<List<NotificationDto>>>` — first emission is often `Loading`, then `Success` or `Error`.

Handle all sealed variants:

```kotlin
when (val r = repository.getSummary(items)) {
    is NetworkResponse.Success -> { /* use r.data */ }
    is NetworkResponse.Error -> { /* show r.message */ }
    NetworkResponse.Loading -> { /* optional */ }
}
```

### 4.2 Do not bypass layers for production code

Avoid calling `NetworkGraph`’s internal `OkHttpNetworkClient` from a ViewModel. If you need a new capability, add a **datasource + repository** API so tests and replacements stay easy.

---

## 5. Adding a new API endpoint

Follow these steps in order.

### Step A — DTOs

Add or extend Kotlin types in `data.remote.dto` that match JSON fields (names must match Gson’s expectations, or add `@SerializedName`).

### Step B — `ApiRequest` implementation

In the appropriate `*Api` class (e.g. `NotificationApi`, or create `SettingsApi`, `UserApi`, …):

1. Add a **`private` class** implementing `ApiRequest<YourDto>`.
2. Set `method`, `path`, optional `query` / `headers`.
3. For POST/PUT JSON, set `body` using `gson.toJson(...).toRequestBody(JsonMediaType)` (see `PostChatCompletionRequest` in `NotificationApi.kt`).
4. Set `responseType` to:
   - `YourDto::class.java` for a single object, or
   - `object : TypeToken<List<YourDto>>() {}.type` for generic collections.

### Step C — Factory method on `*Api`

Expose a function that returns `ApiRequest<T>`:

```kotlin
fun getThing(id: String): ApiRequest<ThingDto> = GetThingRequest(id, gson)
```

### Step D — Remote datasource

On the **`XxxRemoteDataSource`** interface, add a suspend function that calls:

```kotlin
networkClient.execute(yourApi.getThing(id))
```

Implement it in `XxxRemoteDataSourceImpl`.

### Step E — Repository

Map `NetworkResult` to domain models or to `NetworkResponse` as needed. Keep HTTP details and DTO parsing out of the UI.

### Step F — `NetworkGraph`

Wire new `*Api`, datasource, and repository instances the same way `notificationRepository` is built. Expose a new `val` if other components need it.

---

## 6. Configuration

### 6.1 Base URL

Set in `NetworkGraph` companion (`DEFAULT_BASE_URL`). The base URL should end with `/` so relative paths resolve predictably (e.g. `https://api.example.com/` + `notifications`).

For multiple environments, replace the constant with `BuildConfig` fields or flavor-specific resources and pass the chosen URL into a `NetworkGraph` factory.

### 6.2 OkHttp interceptors, timeouts, cache

All belong in the `OkHttpClient.Builder` chain inside `NetworkGraph` (or a future `NetworkModule`). Examples: logging, auth headers, token refresh `Authenticator`, `Cache`, connect/read timeouts.

### 6.3 Gson

A single `Gson` instance is shared. Register `TypeAdapter`s on that instance if you need custom parsing.

---

## 7. Testing

### 7.1 `FakeNetworkClient`

Location: `app/src/test/.../fake/FakeNetworkClient.kt`.

Stub responses by **the same `ApiRequest` instance shape** the production code uses, keyed internally as `"${method}:${path}"`:

```kotlin
val gson = Gson()
val api = NotificationApi(gson)
val fake = FakeNetworkClient().apply {
    stub(
        api.getNotifications(),
        NetworkResult.Success(emptyList()),
    )
}
val dataSource = NotificationRemoteDataSourceImpl(fake, api)
```

If two requests share the same method and path but need different stubs, extend the fake keying strategy (e.g. include a query signature) in one place.

### 7.2 `FakeNotificationRemoteDataSource`

Use when tests should not care about HTTP at all: set `notificationsResult` / summary-related fields, then pass the fake into `NotificationRepositoryImpl`.

### 7.3 Running unit tests

```bash
./gradlew :app:testDebugUnitTest
```

Example tests: `NotificationRepositoryImplTest`, `UserViewModelTest`.

---

## 8. Common pitfalls

1. **Wrong `responseType`** — Gson returns wrong types or fails silently; always use `TypeToken` for lists/maps/generics.
2. **POST without body** — `OkHttpNetworkClient` requires a body for POST/PUT when the builder path expects one; use empty JSON `{}` if the API allows it.
3. **Base URL vs path** — Avoid leading `/` on `path` unless you intend absolute resolution; prefer `path = "v1/resource"` with base `https://host/api/`.
4. **Blocking the UI thread** — Only call `execute` from coroutines (`suspend`); the client uses `Dispatchers.IO` internally for the blocking OkHttp call.
5. **Leaking secrets in logs** — Turn `HttpLoggingInterceptor` down or off in release builds; strip authorization headers from logs if you log them at all.
6. **Token refresh and the same `OkHttpClient`** — If you implement `Authenticator`, avoid deadlock: refresh calls should not synchronously wait on the same client without a dedicated refresh client or careful design.

---

## 9. File map (reference)

| Path | Responsibility |
|------|----------------|
| `data.remote.network.*` | `ApiRequest`, `NetworkClient`, `OkHttpNetworkClient`, `NetworkResult`, `AppError`, mapping helpers |
| `data.remote.request.*` | `*Api` factories building `ApiRequest` instances |
| `data.remote.datasource.*` | Remote IO: `execute(...)` only |
| `data.repository.*` | Mapping, `Flow`, `NetworkResponse`, domain rules |
| `data.remote.di.NetworkGraph` | Wires dependencies for the app |
| `NotiTrackerApp` | Creates `NetworkGraph` at startup |

---

## 10. Related example (User flow)

For a minimal **GET** + **Flow** + **ViewModel** + **StateFlow** example (separate from notifications), see:

- `data.remote.request.UserApi`
- `data.repository.UserRepository` / `UserRepositoryImpl`
- `presentation.UserViewModel`

Wire `UserRepository` through `NetworkGraph` when a screen needs it (same pattern as `notificationRepository`).

---

*If this document drifts from the code, update the table in §9 and the steps in §5 when you add or rename packages.*
