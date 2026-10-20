# ─────────────────────────────────────────────────────────────
# STAGE 1 — BUILD
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Couche de cache : dépendances Maven résolues avant la copie du code source
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B package -DskipTests

# ─────────────────────────────────────────────────────────────
# STAGE 2 — RUNTIME
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre AS runtime

# curl sert au healthcheck (absent de l'image JRE de base)
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# Utilisateur non-root dédié
RUN groupadd --system --gid 10001 app \
 && useradd --system --uid 10001 --gid app --no-create-home app

WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=docker

USER app
EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8081/actuator/health || exit 1

# --enable-preview obligatoire : le code est compilé avec --enable-preview
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
