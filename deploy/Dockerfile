# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
COPY worker-core/pom.xml worker-core/
COPY worker-adapters/pom.xml worker-adapters/
COPY worker-app/pom.xml worker-app/

RUN mvn -B dependency:go-offline

COPY . .
RUN mvn -B clean package -DskipTests

# Final stage
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system telemed \
    && useradd --system --gid telemed --home-dir /app --shell /usr/sbin/nologin telemed

COPY --from=builder --chown=telemed:telemed /app/worker-app/target/worker-app-0.1.0.jar app.jar

USER telemed

ENTRYPOINT ["java", "-jar", "app.jar"]
