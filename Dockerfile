FROM maven:3.9-eclipse-temurin-21-noble AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

FROM alpine:3.20 AS yt-dlp
ARG YT_DLP_VERSION=2026.10.26
RUN apk add --no-cache curl \
    && curl -fsSL -o /yt-dlp "https://github.com/yt-dlp/yt-dlp/releases/download/${YT_DLP_VERSION}/yt-dlp_linux" \
    && curl -fsSL -o /SHA2-256SUMS "https://github.com/yt-dlp/yt-dlp/releases/download/${YT_DLP_VERSION}/SHA2-256SUMS" \
    && echo "$(grep ' yt-dlp_linux$' /SHA2-256SUMS | cut -d' ' -f1)  /yt-dlp" | sha256sum -c - \
    && chmod +x /yt-dlp

FROM eclipse-temurin:21-jre-noble
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 bot && mkdir /data && chown bot /data
COPY --from=yt-dlp /yt-dlp /usr/local/bin/yt-dlp
WORKDIR /app
COPY --from=build /build/target/fraerish-bot.jar app.jar
USER bot
ENV BOT_DATA_DIR=/data
VOLUME /data
ENTRYPOINT ["java", "-Duser.timezone=Europe/Minsk", "-jar", "app.jar"]