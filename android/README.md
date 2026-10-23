# Android app (TWA)

The Android app is a [Trusted Web Activity](https://developer.chrome.com/docs/android/trusted-web-activity):
it opens the same web frontend the Telegram Mini App uses, full-screen, with no URL bar. There is no app code
here beyond [android-browser-helper](https://github.com/GoogleChrome/android-browser-helper)'s launcher, so every
web deploy updates the app. Outside Telegram the first screen is the Telegram Login Widget.

## Build

CI (`.github/workflows/android.yml`) builds the APK on every change under `android/` or by hand (Actions → Android APK
→ Run workflow); builds from `main` are published as GitHub releases `android-1.0.<run>`.

Locally (JDK 17, Android SDK, Gradle 8.11):

```sh
cd android
gradle :app:assembleRelease -Phost=<web app host>     # → app/build/outputs/apk/release/
```

## One-time setup

1. **BotFather → /setdomain** for the bot: the web app's domain, or the Login Widget refuses to load.
2. A signing key, kept out of git (losing it means users must reinstall):
   `keytool -genkeypair -v -keystore classlog.keystore -alias classlog -keyalg RSA -keysize 2048 -validity 10000`
3. GitHub repo settings:
   - variable `CLASSLOG_HOST` — the web app's host, without `https://`;
   - secrets `ANDROID_KEYSTORE_BASE64` (`base64 -w0 classlog.keystore`) and `ANDROID_KEYSTORE_PASSWORD`.
4. Env on the bot:
   - `BOT_USERNAME` — the bot's @username (the login screen shows "unavailable" without it);
   - `ANDROID_CERT_SHA256` — the key's SHA-256 fingerprint (`keytool -list -v -keystore classlog.keystore`),
     served at `/.well-known/assetlinks.json`; without it the app opens with a URL bar.
