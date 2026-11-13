# The image itself is built by Jib; this thin layer only adds the HEALTHCHECK
# instruction, which the Jib Gradle plugin cannot emit.
#
# The probe hits /health: on the bot image ktgbotapi-commons serves it on 8081 (it pings the
# Bot API with getMe for every bot started through longPolling); on the web image the Ktor
# server serves it on 8080. HEALTH_PORT picks the port per image; HEALTHCHECK_PORT still
# overrides it at runtime.
ARG BASE_IMAGE
FROM ${BASE_IMAGE}
ARG HEALTH_PORT=8081
ENV HEALTHCHECK_PORT=${HEALTH_PORT}

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD ["/bin/bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/${HEALTHCHECK_PORT:-8081} && printf 'GET /health HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n' >&3 && head -n 1 <&3 | grep -q ' 200 '"]
