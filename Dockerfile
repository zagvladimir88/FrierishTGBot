FROM maven:3.9-eclipse-temurin-21-noble AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-noble
RUN useradd --system --uid 10001 bot && mkdir /data && chown bot /data
WORKDIR /app
COPY --from=build /build/target/fraerish-bot.jar app.jar
USER bot
ENV BOT_DATA_DIR=/data
VOLUME /data
ENTRYPOINT ["java", "-Duser.timezone=Europe/Minsk", "-jar", "app.jar"]