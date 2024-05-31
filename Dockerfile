FROM gradle:jdk21 as gradle

COPY ./ ./

RUN gradle shadowJar

FROM openjdk:21-slim

WORKDIR /znatokiBot

COPY --from=gradle /home/gradle/build/libs/znatokiStatistic-1.0-SNAPSHOT-all.jar .

RUN apt-get update
RUN apt-get install tzdata curl fontconfig libfreetype6 -y

ENV TZ Asia/Novosibirsk

CMD ["java", "-jar", "znatokiStatistic-1.0-SNAPSHOT-all.jar" ]
