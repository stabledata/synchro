FROM openjdk:17-jdk-slim as build
WORKDIR /app
COPY . .
RUN ./gradlew installDist
RUN ls -la /app/build/install

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/build/install/synchro /app
EXPOSE 8080
ENTRYPOINT ["./bin/synchro"]