# syntax=docker/dockerfile:1
# Image of the API: docker build -t fcc-api apps/api
# Configuration comes from environment variables only; see .env.example for the production settings.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
# Tests and checks run in CI; the image only packages what CI has verified.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp package -DskipTests -Djacoco.skip=true \
    && cp target/fcc-api-*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 --no-create-home fcc
WORKDIR /application
# Layers ordered from least to most often changed, so a code change rebuilds only the last one.
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./
USER fcc
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "application.jar"]
