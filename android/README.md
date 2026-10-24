# Android app

A native app around the web frontend: the frontend's production build is bundled into the APK and served
locally by a WebView (`WebViewAssetLoader`), so nothing opens a browser and the only network traffic is the API
at `https://<host>/api`. Any other request is blocked; links elsewhere (the bot sign-in, phone, WhatsApp) open in
the apps that handle them. A frontend change reaches the app with the next APK, not with a web deploy.

Sign-in is through the bot: «Войти через Telegram» opens `t.me/<bot>?start=login_<token>`, you press Start, and the
app picks up the session (30 days).

## Build

CI (`.github/workflows/android.yml`) builds the APK on every change under `android/` or `web-frontend/`, or by hand
(Actions → Android APK → Run workflow); builds from `main` are published as GitHub releases `android-1.0.<run>`.

Locally (JDK 17, Android SDK, Gradle 8.11):

```sh
./gradlew :web-frontend:jsBrowserDistribution              # in the repo root
cd android
gradle :app:assembleRelease -Phost=<API host>              # → app/build/outputs/apk/release/
```

## One-time setup

1. A signing key, kept out of git (losing it means users must reinstall):
   `keytool -genkeypair -v -keystore classlog.keystore -alias classlog -keyalg RSA -keysize 2048 -validity 10000`
2. GitHub repo settings:
   - variable `CLASSLOG_HOST` — the API's host, without `https://`;
   - secrets `ANDROID_KEYSTORE_BASE64` (`base64 -w0 classlog.keystore`) and `ANDROID_KEYSTORE_PASSWORD`.
