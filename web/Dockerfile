FROM eclipse-temurin:24-jre

# Add JVM flags for performance
ENV JAVA_OPTS="--add-opens=java.base/java.lang=ALL-UNNAMED --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED"

# Set working directory
WORKDIR /app

# Copy the built jar
COPY web/build/libs/web-jvm.jar /app/app.jar

# Expose port
EXPOSE 8080

# Add labels
LABEL org.opencontainers.image.source=""
LABEL org.opencontainers.image.revision=""
LABEL org.opencontainers.image.description="Web application module"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
