![MTProto Hub Banner](mtprotohub-banner.png)

# MTProto Hub

[![Android](https://img.shields.io/badge/Android-24%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

MTProto Hub is an Android app that fetches MTProto proxy links, checks which ones are alive, keeps the results in a local database, and exposes a local `tg://proxy` link for Telegram. The app can also run a foreground service that relays and transcodes traffic through a selected working proxy.

This repository is a practical Android/Kotlin codebase, not a Telegram client. Its job is to manage proxy sources, rank working proxies, act as an active MTProto crypto-gateway, and give Telegram a local endpoint to connect to.

## Installation and Updates

Starting from version 1.1.1, you no longer need to build the project from source to use the application. Pre-compiled stable APKs are available for direct download.

1. Navigate to the **Releases** section of this GitHub repository.
2. Download the latest `.apk` file from the release assets.
3. Install the APK on your Android device (you may need to allow installations from unknown sources in your device settings).

**Updating the application:**
To update the app to a newer version, simply download the latest release APK and install it over your current installation. All your settings, proxy lists, custom secrets, and local database records will be automatically preserved. You can also use the in-app update checker if enabled in the settings.

## What it does

- fetches proxy lists from a configurable remote URL
- parses `tg://proxy` and `https://t.me/proxy` style links
- tests proxies with TCP connection checks
- stores proxy data and logs in Room
- ranks proxies by score, latency, and success rate
- supports static custom or device-generated MTProto secrets
- starts a local foreground service on `127.0.0.1` and acts as a crypto-gateway translating local traffic to upstream servers
- exports both the original proxy link and the local Telegram link
- shows logs, connected clients, settings, and a simple help flow

## Main flow

1. The app loads a proxy source from `Settings`.
2. `ProxyManager` downloads and parses the list.
3. `ProxyChecker` tests each proxy and updates its status.
4. The best working proxy is shown in the UI.
5. `LocalProxyService` listens on a local port. It accepts incoming Telegram traffic using a static local secret, decrypts it, and re-encrypts it using the remote proxy's secret before forwarding.
6. Telegram connects to the local `tg://proxy` link instead of using a remote proxy directly, ensuring stable connections even if the background proxy switches.

## Project structure

- `app/src/main/java/com/example/MainActivity.kt` — entry point and Compose shell
- `app/src/main/java/com/example/ui/` — screens, navigation, and ViewModel
- `app/src/main/java/com/example/proxy/` — parsing, checking, and refresh logic
- `app/src/main/java/com/example/service/` — local relay foreground service and crypto-gateway logic
- `app/src/main/java/com/example/data/` — Room database, DAO, network client, and settings storage
- `app/src/main/java/com/example/models/` — proxy and log entities
- `app/src/main/java/com/example/utils/` — cryptographic utilities and device secret generation

## Requirements

- Android Studio with Kotlin/Compose support (for building from source)
- Android SDK 24 or newer
- Internet access for proxy source fetching
- A proxy source URL that returns plain text lines with valid proxy links

## Build and run (For developers)

1. Open the project in Android Studio.
2. Sync Gradle.
3. Run the `debug` or `release` variant on a device or emulator.
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
- local relay and crypto logic: `service/LocalProxyService.kt`, `utils/MtprotoCipher.kt`
- persisted settings: `data/SettingsRepository.kt`
- schema and queries: `data/AppDatabase.kt`, `data/ProxyDao.kt`, `data/LogDao.kt`

## Reporting issues

If you encounter a bug, have a feature request, or notice something missing, please open an issue in the repository issue tracker.

To help resolve the problem quickly, a good issue report must include:

- A short, descriptive title
- Android version and exact device model
- Application version or specific commit hash
- Exact step-by-step instructions to reproduce the issue
- What you expected to happen
- What actually happened
- Relevant logs from the in-app log screen
- Screenshots (if the issue is related to the UI)
- The source URL being used (if the bug depends on the proxy feed)

Please keep the report focused on one specific problem. If you have multiple distinct bugs, open separate issues for each.

## Contributing

Contributions are welcome and appreciated. To maintain the quality and stability of the codebase, please follow these steps:

1. Fork the repository or create a new branch from `main`.
2. Make a single, focused change per pull request. Do not mix unrelated features or fixes.
3. Keep UI and business logic changes small, clean, and readable.
4. Run the application and thoroughly check the affected user flow.
5. Update the documentation and README if the application behavior has changed.
6. Open a pull request with a clear summary of your changes.

Preferred Pull Request content should include:

- A description of what exactly changed
- The reasoning behind why it was changed
- Clear instructions on how reviewers can test your changes
- Screenshots or log outputs, when relevant to the changes made

## License

MIT. See [LICENSE](LICENSE).

## Acknowledgements

Special thanks to [SoliSpirit/mtproto](https://github.com/SoliSpirit/mtproto) for providing the MTProto proxies used by this application, as well as for keeping them updated and maintained. This helps keep the project stable and ensures the available connections stay up to date.