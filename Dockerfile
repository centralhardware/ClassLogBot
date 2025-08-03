FROM gradle:jdk21 as gradle

COPY ./ ./

ENV DEFAULT_JVM_OPTS='"--add-modules jdk.incubator.vector"'
RUN gradle installDist

FROM openjdk:21-slim

WORKDIR /znatokiBot

COPY --from=gradle /home/gradle/build/install/znatokiStatistic/ .

RUN apt-get update && apt-get install -y tzdata fontconfig libfreetype6 && apt-get clean && rm -rf /var/lib/apt/lists/*

ENV TZ Asia/Novosibirsk

CMD ["bin/znatokiStatistic"]
