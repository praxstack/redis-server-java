FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends git curl maven redis-tools \
    && rm -rf /var/lib/apt/lists/*
