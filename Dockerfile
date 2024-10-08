FROM gradle:jdk23 as gradle

COPY ./ ./

RUN gradle shadowJar

FROM openjdk:23-slim

WORKDIR /znatokiBot

COPY --from=gradle /home/gradle/build/libs/shadow-1.0-SNAPSHOT-all.jar .

RUN apt-get update
RUN apt-get update && apt-get install -y tzdata curl fontconfig libfreetype6 && apt-get clean && rm -rf /var/lib/apt/lists/*

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl --fail http://localhost:81/health || exit 1

ENV TZ Asia/Novosibirsk

CMD ["java", "-jar", "shadow-1.0-SNAPSHOT-all.jar" ]
