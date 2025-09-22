# ====== Build ======
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
RUN apk add --no-cache bash findutils
COPY . .
RUN sed -i 's/\r$//' gradlew
RUN chmod +x ./gradlew
RUN sh ./gradlew :app-service:bootJar --no-daemon

# ====== Runtime ======
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/applications/app-service/build/libs/loan-application.jar /app/app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8082
USER app
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
