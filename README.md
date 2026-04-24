# NotiTracker

Android app (Kotlin + Jetpack Compose) that listens for notifications and talks to backend APIs (notification list, chat completion / summarization).

## Table of contents

- [Requirements](#requirements)
- [Build & run](#build--run)
- [Tests](#tests)
- [Using the network layer](#using-the-network-layer)
- [Code examples: passing data and handling results](#code-examples-passing-data-and-handling-results)
- [Adding a new API (checklist)](#adding-a-new-api-checklist)
- [Further documentation](#further-documentation)
- [Project layout](#project-layout)

---

## Requirements

- Android Studio (AGP version compatible with this project)
- A JDK compatible with Gradle / AGP (often JDK 11 or 17, depending on your machine)

## Build & run

```bash
./gradlew :app:assembleDebug
```

Run on a device or emulator and enable the **Notification listener** permission in system settings.

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

---

## Using the network layer

**Rule:** screens, `Service`, and `ViewModel` classes must **not** call `OkHttp` or `NetworkClient` directly. Call a **repository interface** only. The repository calls the **remote data source**, which runs `networkClient.execute(apiRequest)`.

```text
UI / Service / ViewModel
        │  pass arguments (e.g. List<NotificationDto>, userId)
        ▼
NotificationRepository  (interface)
        │  build prompts / DTOs internally when needed
        ▼
NotificationRemoteDataSource
        │  execute(NotificationApi.postChatCompletion(...))
        ▼
NetworkClient  →  HTTP  →  JSON  →  DTO
```

**Getting a repository (manual DI today):**

1. `AndroidManifest.xml` registers `android:name=".NotiTrackerApp"`.
2. From an `Activity`, `Service`, or `Application`:

```kotlin
val repo = (application as NotiTrackerApp).networkGraph.notificationRepository
```

**Two result types:**

| Type | When to use it |
|------|----------------|
| `NetworkResponse<T>` | `Loading` / `Success` / `Error` — good for UI, especially with `Flow`. |
| `NetworkResult<T>` | `Success` / `Failure(AppError)` — used deeper in the data layer; can be mapped to `NetworkResponse`. |

**Server configuration:** the base URL lives in `NetworkGraph` (`DEFAULT_BASE_URL`). Change it per environment (ngrok, staging, production).

---

## Code examples: passing data and handling results

### 1) Suspend call with input — `getSummary` (list of notifications)

The repository takes a **`List<NotificationDto>`**. Inside the repository, that list is turned into **`List<ChatMessage>`** and `postChatCompletion` is invoked — you only pass the DTO list from UI or a service:

```kotlin
import com.example.notitracker.NotiTrackerApp
import com.example.notitracker.data.remote.NetworkResponse
import com.example.notitracker.data.remote.dto.NotificationDto

suspend fun processBatch(application: android.app.Application, notifications: List<NotificationDto>) {
    val repo = (application as NotiTrackerApp).networkGraph.notificationRepository

    when (val response = repo.getSummary(notifications)) {
        is NetworkResponse.Success -> {
            val summary = response.data.summary
            val replies = response.data.suggestedReplies
            // use summary, replies
        }
        is NetworkResponse.Error -> {
            // response.message, response.code
        }
        NetworkResponse.Loading -> {
            // rare for a one-shot suspend call
        }
    }
}
```

**Data path:** `notifications` → `getSummary` → repository builds prompt + `ChatMessage` list → JSON `ChatRequest` in `NotificationApi` → POST `v1/chat/completions` → parse `ChatResponse` → map to `SummaryResponse`.

### 2) GET exposed as `Flow` — `getNotifications`

No request body. `collect` the flow to observe `Loading`, then `Success` or `Error`:

```kotlin
import com.example.notitracker.data.remote.NetworkResponse
import com.example.notitracker.data.remote.dto.NotificationDto
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

someScope.launch {
    repo.getNotifications().collect { state ->
        when (state) {
            is NetworkResponse.Loading -> { /* in progress */ }
            is NetworkResponse.Success -> {
                val list: List<NotificationDto> = state.data
            }
            is NetworkResponse.Error -> { /* state.message, state.code */ }
        }
    }
}
```

### 3) ViewModel (Compose) — pass data and update UI

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notitracker.data.repository.NotificationRepository
import com.example.notitracker.data.remote.NetworkResponse
import com.example.notitracker.data.remote.dto.NotificationDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val repository: NotificationRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow<NetworkResponse<List<NotificationDto>>?>(null)
    val ui = _ui.asStateFlow()

    fun load() {
        viewModelScope.launch {
            repository.getNotifications().collect { response ->
                _ui.value = response
            }
        }
    }

    fun summarize(batch: List<NotificationDto>) {
        viewModelScope.launch {
            when (val r = repository.getSummary(batch)) {
                is NetworkResponse.Success -> { /* update summary state */ }
                is NetworkResponse.Error -> { /* handle error */ }
                NetworkResponse.Loading -> Unit
            }
        }
    }
}

// Without Hilt, from an Activity/Fragment for now:
// val repo = (application as NotiTrackerApp).networkGraph.notificationRepository
// val vm = NotificationsViewModel(repo)
```

With **Hilt**, inject `NotificationRepository` into the `ViewModel` instead of constructing it manually.

### 4) `UserRepository` — passing **`userId`** (example GET user)

`UserRepository` / `UserRepositoryImpl` exist in the codebase but are **not** wired in `NetworkGraph` yet. To use them:

1. In `NetworkGraph`: create `UserApi()`, `UserRepositoryImpl(networkClient, userApi)`, expose `val userRepository: UserRepository`.
2. Call `userRepository.getUser(userId)` — returns `Flow<NetworkResult<User>>` (no built-in `Loading`; you can emit a loading state in the `ViewModel` if you want the same UX as notifications).

---

## Adding a new API (checklist)

1. Add or adjust DTOs under `data.remote.dto`.
2. In `NotificationApi` (or a new `XxxApi` file): add a `private` class implementing `ApiRequest<T>` — set `method`, `path`, optional `query` / `headers`, `body` (JSON for POST), and `responseType` (use Gson `TypeToken` for `List<...>`).
3. Add a method on `NotificationRemoteDataSource` + impl: `networkClient.execute(api.yourMethod(...))`.
4. Add a method on `NotificationRepository` + map `NetworkResult` → `NetworkResponse` when the UI needs the loading/error model.
5. Register new types in `NetworkGraph` if you introduce new APIs or repositories.

Architecture details, pitfalls, and `FakeNetworkClient` testing: **[docs/NETWORK_LAYER.md](docs/NETWORK_LAYER.md)**.

---

## Further documentation

| Doc | Contents |
|-----|----------|
| [docs/NETWORK_LAYER.md](docs/NETWORK_LAYER.md) | Full architecture, `ApiRequest`, `FakeNetworkClient`, common pitfalls |

## Project layout

| Path / package | Role |
|----------------|------|
| `app/.../data/remote` | HTTP: `NetworkClient`, `OkHttpNetworkClient`, `*Api`, DTOs |
| `app/.../data/repository` | Repositories, mapping results for UI |
| `app/.../data/remote/di/NetworkGraph` | OkHttp, Gson, repository wiring |
| `app/.../service` | `NotificationListenerService` |
| `app/.../presentation` | Sample `UserViewModel` |
| `docs/` | In-depth network layer guide |
