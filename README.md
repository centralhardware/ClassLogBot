# ClassLogBot

This was never really intended to be an open-source project.  
But I don’t see any point in keeping the source code private — there’s nothing here worth hiding.  
So, here it is.

The project is a simple tool for managing students and lessons in a tutoring center.  
It’s not built as a product, more like a working utility.

If you find anything interesting, feel free to open an [Issue](../../issues).  
I’ll be glad to hear your questions, suggestions, or feedback.

---

### What’s inside
- student management  
- lesson tracking  
- payment tracking  

---

### Web app (Telegram Mini App)

The image also serves a Telegram Mini App: lessons, schedule, students, payments, monthly
reports (with hand-in to the director), stats, team and permissions, and an audit log.

- `app/` — the UI, Compose Multiplatform: Wasm in the browser (with a JS fallback for webviews without WasmGC), and the
  native Android app in `androidApp/`. The web build is packed into the bot jar under `/web`.
- `bot/.../web/` — the Ktor API (`/auth`, `/api/**`) and static hosting, started by `AppMain` before the bot.
- Sign-in uses Telegram's `initData` (HMAC-checked with `BOT_TOKEN`); access needs `WEB_INTERFACE`.
- New permissions: `DIRECTOR` (reviews reports, read-only) and `MANAGE_TUTORS`.

| Variable | Meaning | Default |
|---|---|---|
| `WEB_PORT` | Port of the web server | `8080` |
| `WEB_ENABLED` | `false` runs the bot only | `true` |

Set the bot's menu button in BotFather (Bot Settings → Menu Button) to the app's https address. That opens the app, and the web server reads the same address for the buttons in its messages.
