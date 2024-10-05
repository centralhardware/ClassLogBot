FROM gradle:jdk21 as gradle

COPY ./ ./

RUN gradle shadowJar

FROM openjdk:21-slim

WORKDIR /znatokiBot

COPY --from=gradle /home/gradle/build/libs/shadow-1.0-SNAPSHOT-all.jar .

RUN apt-get update
RUN apt-get update && apt-get install -y install tzdata curl fontconfig libfreetype6 && apt-get clean && rm -rf /var/lib/apt/lists/*

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
  CMD curl --fail http://localhost:80/health || exit 1

ENV TZ Asia/Novosibirsk

CMD ["java", "-jar", "shadow-1.0-SNAPSHOT-all.jar" ]
