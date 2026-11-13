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

A separate image serves a Telegram Mini App: lessons, schedule, students, payments, monthly
reports (with hand-in to the director), stats, team and permissions, and an audit log.

The project builds two images from one repo, sharing the database and `common/`:

- `ghcr.io/<repo>:bot-latest` — `bot/`, the Telegram bot (`AppMain`), healthcheck on `:8081/health`.
- `ghcr.io/<repo>:web-latest` — `web/`, the Ktor API (`/auth`, `/api/**`) and static hosting (`WebMain`),
  healthcheck on `:8080/health`. It sends Telegram messages with `BOT_TOKEN` but never polls updates.
- `web-frontend/` — the UI, Compose HTML (Kotlin/JS). Its build is packed into the web jar under `/web`.
- Both images run the Flyway migrations at startup; both need the same database, MinIO and `BOT_TOKEN` settings.
- "Sign in via the bot" tokens live in the `bot_login` table, since the web creates them and the bot confirms them.
- Sign-in uses Telegram's `initData` (HMAC-checked with `BOT_TOKEN`); access needs `WEB_INTERFACE`.
- New permissions: `DIRECTOR` (reviews reports, read-only) and `MANAGE_TUTORS`.

| Variable | Meaning | Default |
|---|---|---|
| `WEB_PORT` | Port of the web server | `8080` |

Set the bot's menu button in BotFather (Bot Settings → Menu Button) to the app's https address. That opens the app, and the web server reads the same address for the buttons in its messages.
