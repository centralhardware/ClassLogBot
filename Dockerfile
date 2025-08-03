FROM gradle:jdk24 as gradle

COPY ./ ./

RUN gradle shadowJar

FROM openjdk:24-slim

WORKDIR /znatokiBot

COPY --from=gradle /home/gradle/build/libs/shadow-1.0-SNAPSHOT-all.jar .

RUN apt-get update && apt-get install -y tzdata fontconfig libfreetype6 && apt-get clean && rm -rf /var/lib/apt/lists/*

ENV TZ Asia/Novosibirsk

CMD ["java","--add-modules", "jdk.incubator.vector",  "-jar", "shadow-1.0-SNAPSHOT-all.jar"]
