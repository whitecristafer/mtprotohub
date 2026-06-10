# MTProto Hub

[![Android](https://img.shields.io/badge/Android-24%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

MTProto Hub is an Android app that fetches MTProto proxy links, checks which ones are alive, keeps the results in a local database, and exposes a local `tg://proxy` link for Telegram. The app can also run a foreground service that relays traffic through a selected working proxy.

This repository is a practical Android/Kotlin codebase, not a Telegram client. Its job is to manage proxy sources, rank working proxies, and give Telegram a local endpoint to connect to.

## What it does

- fetches proxy lists from a configurable remote URL
- parses `tg://proxy` and `https://t.me/proxy` style links
- tests proxies with TCP connection checks
- stores proxy data and logs in Room
- ranks proxies by score, latency, and success rate
- starts a local foreground service on `127.0.0.1`
- exports both the original proxy link and the local Telegram link
- shows logs, connected clients, settings, and a simple help flow

## Main flow

1. The app loads a proxy source from `Settings`.
2. `ProxyManager` downloads and parses the list.
3. `ProxyChecker` tests each proxy and updates its status.
4. The best working proxy is shown in the UI.
5. `LocalProxyService` listens on a local port and forwards traffic to the selected proxy.
6. Telegram connects to the local `tg://proxy` link instead of using a remote proxy directly.

## Project structure

- `app/src/main/java/com/example/MainActivity.kt` — entry point and Compose shell
- `app/src/main/java/com/example/ui/` — screens, navigation, and ViewModel
- `app/src/main/java/com/example/proxy/` — parsing, checking, and refresh logic
- `app/src/main/java/com/example/service/` — local relay foreground service
- `app/src/main/java/com/example/data/` — Room database, DAO, network client, and settings storage
- `app/src/main/java/com/example/models/` — proxy and log entities

## Requirements

- Android Studio with Kotlin/Compose support
- Android SDK 24 or newer
- Internet access for proxy source fetching
- A proxy source URL that returns plain text lines with valid proxy links

## Build and run

1. Open the project in Android Studio.
2. Sync Gradle.
3. Run the `debug` variant on a device or emulator.
4. Open the app and press **Fetch Now** or **Check Pings**.
5. Start the gateway from the home screen.
6. Copy the local `tg://proxy` link and paste it into Telegram.

## Development notes

- The app uses Jetpack Compose, Room, Ktor, and coroutines.
- Proxy data is kept in the local Room database.
- Logging goes through Timber and is also written into the `logs` table.
- `LocalProxyService` is a foreground service, so Android may show a persistent notification while it runs.
- Release signing reads values from environment variables defined in `app/build.gradle.kts`.

## Where to start when changing code

- UI behavior: `ui/home`, `ui/proxies`, `ui/settings`
- source fetching and ranking: `proxy/ProxyManager.kt`
- proxy validation: `proxy/ProxyChecker.kt`
- local relay logic: `service/LocalProxyService.kt`
- persisted settings: `data/SettingsRepository.kt`
- schema and queries: `data/AppDatabase.kt`, `data/ProxyDao.kt`, `data/LogDao.kt`

## Reporting issues

Use an issue when something is broken, confusing, or missing.

A good issue includes:

- short title
- Android version and device model
- app version or commit hash
- exact steps to reproduce
- what you expected
- what happened instead
- relevant logs from the app log screen
- screenshot if the UI is involved
- source URL if the bug depends on the proxy feed

Keep the report focused on one problem. If you have more than one bug, open separate issues.

## Contributing

1. Fork or branch from `main`.
2. Make one focused change.
3. Keep UI and logic changes small and readable.
4. Run the app and check the affected flow.
5. Update docs if behavior changed.
6. Open a pull request with a clear summary and reproduction notes.

Preferred PR content:

- what changed
- why it changed
- how to test it
- screenshots or logs when relevant

## License

MIT. See [LICENSE](LICENSE).

## Acknowledgements

Special thanks to [SoliSpirit/mtproto](https://github.com/SoliSpirit/mtproto) for providing the MTProto proxies used by this application, as well as for keeping them updated and maintained. This helps keep the project stable and ensures the available connections stay up to date.