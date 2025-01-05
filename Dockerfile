FROM openjdk:18-ea-21-jdk-slim-buster as build
WORKDIR /app
COPY . .
RUN ./gradlew installDist
RUN ls -la /app/build/install/synchro

FROM openjdk:18-ea-21-jdk-slim-buster
WORKDIR /app
COPY --from=build /app/build/install/synchro /app
EXPOSE 8080
ENTRYPOINT ["./bin/synchro"]