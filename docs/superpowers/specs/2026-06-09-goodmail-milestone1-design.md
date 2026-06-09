# Goodmail — Milestone 1 Design (Phases 1+2)

**Date:** 2026-06-09
**App:** Goodmail (Android, Kotlin, Jetpack Compose, Material 3)
**Package:** `com.example.goodmail` (kept; never published)
**Status:** Approved for implementation

## Goal

Sign in with Google (granting Gmail scopes) → see the real Gmail INBOX as a list → tap to
read the full email → swipe to trash (with undo). State survives relaunch (skip sign-in if an
account is remembered).

**Out of scope this milestone:** Grok classification, user rules, notifications, background
sync. Those are later cycles, each with its own spec.

## Locked decisions

- **Audience: just the developer.** OAuth stays in **test mode forever** — no Google
  verification, no CASA security assessment. `logassure@gmail.com` is added as a test user.
  A one-time "unverified app → continue" consent warning is expected and acceptable.
- **Auth strategy = Option A:** Gmail Java API client + `GoogleAccountCredential` for
  **automatic token refresh**. We never store or manage an OAuth access token — only the
  selected account email is persisted. This deletes the "encrypted token storage" task.
- **Identity:** package `com.example.goodmail`, display name "Goodmail", minSdk 24,
  targetSdk 36. Must be stable before the Google Cloud OAuth client is created.
- **Single source of truth = Room.** UI always reads from Room; the network only refreshes it.
- **Dark Material 3 only** (no light theme, no dynamic color).

## Required external setup (manual, by developer)

1. Google Cloud Console → project → enable Gmail API.
2. OAuth consent screen: External, **Testing**, add `logassure@gmail.com` as test user.
   Scopes: `gmail.readonly`, `gmail.modify`.
3. OAuth 2.0 Client ID, type **Android**: package `com.example.goodmail` + debug SHA-1
   (`keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`).

## Architecture

```
ui (Compose)  ->  ViewModel  ->  EmailRepository  ->  { GmailService, EmailDao(Room) }
                                       ^                         |
                                       └── single source of truth: Room
auth:  AuthScreen -> AuthViewModel -> GoogleSignIn(+scopes) -> GoogleAccountCredential
                                          -> AccountStore (DataStore: email only)
```

Hilt provides: `GoogleAccountCredential`, `Gmail` service, `AppDatabase`/`EmailDao`,
`AccountStore`, repositories.

### Auth flow (Option A)

- `GoogleSignIn` with `requestScopes(gmail.readonly, gmail.modify)` + `requestEmail()`
  performs identity **and** scope grant in one consent. (`GoogleSignIn` is deprecated but
  functional; acceptable for a personal app. Exact call surface confirmed against live docs
  at implementation time.)
- On success, set `GoogleAccountCredential.usingOAuth2(context, scopes).selectedAccount =
  account.account`. That credential auto-refreshes access tokens on every Gmail call.
- Persist **only** the account email in DataStore. On relaunch: if an email is stored and the
  account is silently available, go to Inbox; else show Auth.
- Sign-out: `googleSignInClient.signOut()` + clear stored email + clear Room.

### Data layer (single source of truth = Room)

- **`GmailService`** (IO dispatcher, wraps Gmail Java client; `NetHttpTransport`, no Apache):
  - `listInbox(maxResults = 50, pageToken?)` → `messages.list(labelIds=["INBOX"])`, then
    fetch each id **metadata-only** (`From`, `Subject`, `Date` headers + `snippet` +
    `internalDate` + `labelIds`). Bodies are **not** fetched here.
  - `getMessage(id)` → full payload; walk MIME parts, prefer `text/html` then `text/plain`,
    base64url-decode.
  - `trash(id)` → `messages.trash`.
- **Room:** `EmailEntity(id, threadId, from, subject, snippet, body?, timestamp, isRead,
  labelIds(JSON string), importance = "UNCLASSIFIED")`. The `importance` column exists now so
  Phase 3 needs no migration. `EmailDao`: upsert, `getAllFlow()`, delete(id), markRead(id),
  clearAll().
- **`EmailRepository`:** `refreshInbox()` (fetch → upsert), `emails: Flow<List<Email>>` from
  Room, `deleteEmail(id)` = optimistic remove + `trash` on Gmail (rollback on failure → powers
  undo), `loadBody(id)` fills the cached body on detail open.

### UI (Compose, dark Material 3)

- **Nav:** `auth` → `inbox` → `detail/{id}`. One `@HiltViewModel` per screen.
- **AuthScreen:** centered "Goodmail" + "Sign in with Google"; launches sign-in intent.
- **InboxScreen:** `LazyColumn` of `EmailListItem` (first-letter avatar, sender, subject,
  snippet, relative time; unread = bolder). `PullToRefreshBox`. `SwipeToDismiss` → trash + undo
  snackbar. Loading + empty states.
- **DetailScreen:** header (from/subject/date) + body in `AndroidView(WebView)` with injected
  dark CSS (dark bg, light text, `max-width:100%`). Top-bar trash + back.

### Error handling

- Sign-in cancelled/failed → stay on Auth with a message.
- `UserRecoverableAuthIOException` (consent revoked/expired) → surface recovery intent or bounce
  to Auth.
- Network failure on refresh → keep cached Room data + dismissible banner (offline works).
- Trash fails → roll back the optimistic delete and notify.

### Testing

- Unit (TDD on pure logic): MIME body extraction (html/plain/nested), `From` header parsing
  (name vs bare email), relative-timestamp formatting, repository upsert/delete with fake
  DAO + fake `GmailService`.
- No instrumented UI tests this milestone (personal app; manual checklist).

## Build / tooling notes

- Existing toolchain: AGP `9.0.1`, Kotlin `2.0.21`, Compose BOM, version catalog
  (`gradle/libs.versions.toml`). Add deps there.
- New plugins: KSP (`2.0.21-1.0.28`, matched to Kotlin), Hilt, kotlinx-serialization.
- New libs: Hilt + hilt-navigation-compose, Room (+KSP compiler), Navigation Compose,
  lifecycle viewmodel/runtime-compose, DataStore Preferences, coroutines (+play-services),
  play-services-auth, google-api-client-android, google-api-services-gmail.
- `packaging.resources.excludes` for `META-INF/*` conflicts from the Google client libs.
- Build/verify with the Android Studio JBR (`C:\Program Files\Android\Android Studio\jbr`,
  JDK 21).

## Feature breakdown (one git commit + push each)

1. Design spec (this doc).
2. Build foundation: catalog deps, plugins, manifest (INTERNET), `GoodmailApp` (`@HiltAndroidApp`), dark theme.
3. Auth: GoogleSignIn + scopes, `GoogleAccountCredential`, `AccountStore`, AuthScreen + VM, nav.
4. Gmail data layer: `GmailService`, Room (entity/dao/db), domain models, `EmailRepository`, DI.
5. Inbox UI: list, pull-to-refresh, swipe-to-trash + undo, VM.
6. Detail UI: WebView body, trash, back.
