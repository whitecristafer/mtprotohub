# MTProto Hub

[![Android](https://img.shields.io/badge/Android-24%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

MTProto Hub — Android-приложение, которое получает список MTProto-прокси, проверяет их доступность, хранит результаты локально и отдаёт Telegram локальную ссылку `tg://proxy`. Приложение также умеет запускать foreground service и проксировать трафик через выбранный рабочий proxy.

Это не Telegram-клиент. Это сервисная Android/Kotlin-утилита для управления источниками прокси, отбора рабочих узлов и выдачи локальной точки входа для Telegram.

## Что умеет проект

- загружает список прокси из настраиваемого URL
- парсит ссылки вида `tg://proxy` и `https://t.me/proxy`
- проверяет прокси через TCP-подключение
- сохраняет прокси и логи в Room
- сортирует прокси по score, latency и success rate
- поднимает локальный foreground service на `127.0.0.1`
- экспортирует оригинальную ссылку и локальную ссылку для Telegram
- показывает логи, подключённых клиентов, настройки и экран помощи

## Как устроен сценарий работы

1. В настройках задан источник списка прокси.
2. `ProxyManager` скачивает и парсит список.
3. `ProxyChecker` проверяет узлы и обновляет их статус.
4. В интерфейсе показывается лучший рабочий proxy.
5. `LocalProxyService` слушает локальный порт и пересылает трафик в выбранный proxy.
6. Telegram подключается к локальной `tg://proxy` ссылке, а не к удалённому адресу напрямую.

## Структура проекта

- `app/src/main/java/com/example/MainActivity.kt` — точка входа и Compose-оболочка
- `app/src/main/java/com/example/ui/` — экраны, навигация и ViewModel
- `app/src/main/java/com/example/proxy/` — парсинг, проверка и обновление списка
- `app/src/main/java/com/example/service/` — foreground service локального relay
- `app/src/main/java/com/example/data/` — Room база, DAO, сеть и хранение настроек
- `app/src/main/java/com/example/models/` — сущности прокси и логов

## Требования

- Android Studio с поддержкой Kotlin/Compose
- Android SDK 24 или выше
- доступ в интернет для загрузки списка прокси
- URL источника, который отдаёт текст со строками прокси-ссылок

## Сборка и запуск

1. Открой проект в Android Studio.
2. Синхронизируй Gradle.
3. Запусти `debug`-сборку на устройстве или эмуляторе.
4. В приложении нажми **Fetch Now** или **Check Pings**.
5. Запусти gateway с главного экрана.
6. Скопируй локальную `tg://proxy` ссылку и вставь её в Telegram.

## Что важно при разработке

- В проекте используются Jetpack Compose, Room, Ktor и coroutines.
- Данные прокси хранятся в локальной Room базе.
- Логи пишутся через Timber и одновременно попадают в таблицу `logs`.
- `LocalProxyService` работает как foreground service, поэтому Android может показывать постоянное уведомление.
- Для release-сборки используются переменные окружения из `app/build.gradle.kts`.

## С чего начинать, если нужно менять код

- UI: `ui/home`, `ui/proxies`, `ui/settings`
- загрузка и сортировка прокси: `proxy/ProxyManager.kt`
- проверка прокси: `proxy/ProxyChecker.kt`
- локальный relay: `service/LocalProxyService.kt`
- настройки: `data/SettingsRepository.kt`
- схема и запросы БД: `data/AppDatabase.kt`, `data/ProxyDao.kt`, `data/LogDao.kt`

## Как оставлять issue

Issue нужен, если что-то сломалось, ведёт себя странно или не хватает функциональности.

Хороший issue содержит:

- короткий заголовок
- версию Android и модель устройства
- версию приложения или commit hash
- точные шаги воспроизведения
- ожидаемое поведение
- фактическое поведение
- логи из экрана логов приложения
- скриншот, если проблема видна в UI
- URL источника, если ошибка зависит от proxy feed

Один issue = одна проблема. Если багов несколько, лучше открыть отдельные записи.

## Как контрибьютить

1. Сделай fork или отдельную branch от `main`.
2. Меняй только одну понятную вещь за раз.
3. Держи UI и бизнес-логику читаемыми.
4. Запусти приложение и проверь связанный сценарий.
5. Обнови документацию, если меняется поведение.
6. Открой pull request с понятным описанием.

Что полезно указать в PR:

- что изменено
- зачем это сделано
- как проверить
- скриншоты или логи, если они нужны

## Лицензия

MIT. См. файл [LICENSE](LICENSE).

## Благодарности

Отдельная благодарность репозиторию [SoliSpirit/mtproto](https://github.com/SoliSpirit/mtproto) за предоставляемые MTProto-прокси, которые используются в работе приложения, а также за их регулярное обновление и поддержку. Это помогает поддерживать стабильную работу проекта и актуальность доступных подключений.****