FROM openjdk:21-slim as maven

COPY ./ ./

RUN apt-get -y update
RUN apt-get install maven -y
RUN mvn package

FROM openjdk:21-slim

WORKDIR /znatokiBot

COPY --from=maven target/znatokiStatistic-1.0-SNAPSHOT.jar .

RUN apt-get update -y && \
    apt-get upgrade -y && \
    apt-get install tzdata curl fontconfig libfreetype6 -y

ENV TZ Asia/Novosibirsk

CMD ["java", "-jar", "znatokiStatistic-1.0-SNAPSHOT.jar" ]
