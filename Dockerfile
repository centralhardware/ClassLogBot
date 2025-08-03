FROM gradle:jdk24 as gradle

COPY ./ ./

RUN gradle installDist

FROM openjdk:24-slim

WORKDIR /znatokiBot

COPY --from=gradle /home/gradle/build/install/znatokiStatistic/ .

RUN apt-get update && apt-get install -y tzdata fontconfig libfreetype6 && apt-get clean && rm -rf /var/lib/apt/lists/*

ENV TZ Asia/Novosibirsk

CMD ["bin/znatokiStatistic", "--add-modules", "jdk.incubator.vector"]
