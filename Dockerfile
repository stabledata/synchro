FROM openjdk:17-jdk-slim as build
WORKDIR /app
COPY . .
RUN ./gradlew installDist
RUN ls -la /app/build/install

FROM openjdk:17-jdk-slim

# Download root certs
RUN apt-get update && apt-get install -y wget
RUN mkdir -p /root/.postgresql && \
    wget https://letsencrypt.org/certs/isrgrootx1.pem -O /root/.postgresql/root.crt
ENV PGSSLROOTCERT /root/.postgresql/root.crt

WORKDIR /app
COPY --from=build /app/build/install/synchro /app
EXPOSE 8080
ENTRYPOINT ["./bin/synchro"]