# Android app

The native app: the same Compose Multiplatform UI as the Mini App (`app/`, its `androidMain` holds the platform
side), in one activity. It talks to the API at `https://<host>/api` directly; links elsewhere (the bot sign-in,
phone, WhatsApp) open in the apps that handle them, photos come from the system photo picker, and generated reports
are saved by the system downloader.

Sign-in is through the bot: «Войти через Telegram» opens `t.me/<bot>?start=login_<token>`, you press Start, and the
app picks up the session (30 days).

## Build

CI (`.github/workflows/android.yml`) builds the APK on every change under `app/` or `androidApp/`, or by hand
(Actions → Android APK → Run workflow); builds from `main` are published as GitHub releases `android-1.0.<run>`.

The module is part of the root build only when an Android SDK is found (`ANDROID_HOME`, `ANDROID_SDK_ROOT` or
`sdk.dir` in `local.properties`); `-PclasslogAndroid=false` leaves it out anyway. Locally (JDK 17+, SDK platform 37):

```sh
./gradlew :androidApp:assembleRelease -Phost=<API host>    # → androidApp/build/outputs/apk/release/
```

## One-time setup

1. A signing key, kept out of git (losing it means users must reinstall):
   `keytool -genkeypair -v -keystore classlog.keystore -alias classlog -keyalg RSA -keysize 2048 -validity 10000`
2. GitHub repo settings:
   - variable `CLASSLOG_HOST` — the API's host, without `https://`;
   - secrets `ANDROID_KEYSTORE_BASE64` (`base64 -w0 classlog.keystore`) and `ANDROID_KEYSTORE_PASSWORD`.
