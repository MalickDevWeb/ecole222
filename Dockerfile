# ── Stage 1 : Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

# Copier les fichiers Maven en premier (optimise le cache Docker)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Télécharger les dépendances séparément (mis en cache si pom.xml ne change pas)
RUN ./mvnw dependency:go-offline -q

# Copier le code source puis builder
COPY src src
RUN ./mvnw package -DskipTests -q

# ── Stage 2 : Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copier uniquement le JAR final
COPY --from=build /workspace/target/*.jar app.jar

# Exposer le port (Render utilisera la variable PORT)
EXPOSE 8080

# Démarrage optimisé JVM pour conteneur
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
