FROM openjdk:18-ea-21-jdk-slim-buster as build
WORKDIR /app
COPY . .
RUN ./gradlew installDist
RUN ls -la /app/build/install/synchro

FROM openjdk:18-ea-21-jdk-slim-buster

# Ensure updated Debian keyring and repositories
RUN apt-get update && apt-get install -y --no-install-recommends debian-archive-keyring && apt-get update

# Install wget and other required tools
RUN apt-get install -y wget

RUN mkdir -p /root/.postgresql && \
    wget https://letsencrypt.org/certs/isrgrootx1.pem -O /root/.postgresql/root.crt
ENV PGSSLROOTCERT /etc/ssl/certs/ca-certificates.crt
WORKDIR /app
COPY --from=build /app/build/install/synchro /app
EXPOSE 8080
ENTRYPOINT ["./bin/synchro"]