FROM gradle:jdk23 as gradle

COPY ./ ./

RUN gradle installDist

FROM openjdk:23-slim

WORKDIR /znatokiBot

COPY --from=gradle /home/gradle/build/install/znatokiStatistic/ ./

RUN apt-get update
RUN apt-get update && apt-get install -y tzdata curl fontconfig libfreetype6 && apt-get clean && rm -rf /var/lib/apt/lists/*

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl --fail http://localhost:81/health || exit 1

ENV TZ Asia/Novosibirsk

CMD ["./bin/znatokiStatistic", "--add-modules", "jdk.incubator.vector"]
