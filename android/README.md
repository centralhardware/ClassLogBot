# Android app (TWA)

The Android app is a [Trusted Web Activity](https://developer.chrome.com/docs/android/trusted-web-activity):
it opens the same web frontend the Telegram Mini App uses, full-screen, with no URL bar. There is no separate
Android code — every web deploy updates the app. Outside Telegram the first screen is the Telegram Login Widget.

## One-time setup

1. **BotFather → /setdomain** for the bot: the site's domain, or the Login Widget refuses to load.
2. Env on the bot:
   - `BOT_USERNAME` — the bot's @username (the login screen shows "unavailable" without it);
   - `ANDROID_CERT_SHA256` — the signing key's SHA-256 fingerprint(s), comma-separated
     (`AB:CD:…`, from `keytool -list -v -keystore classlog.keystore`); served at `/.well-known/assetlinks.json`.
     If the app is distributed via Google Play, add Play's app-signing key fingerprint too.
   - `ANDROID_PACKAGE` — only if the package id differs from `me.centralhardware.classlog`.

## Build

```sh
npm i -g @bubblewrap/cli
cd android
sed -i "s/CLASSLOG_HOST/<the web app's host>/g" twa-manifest.json
bubblewrap update      # generates the Gradle project from twa-manifest.json (first run creates the keystore)
bubblewrap build       # → app-release-signed.apk / app-release-bundle.aab
```

Keep `classlog.keystore` out of git: losing it means a new package id for every user.
