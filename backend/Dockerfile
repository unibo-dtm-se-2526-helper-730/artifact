# ---- Build ---------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Dependencies first: cached until pom.xml changes
COPY .mvn/ .mvn/
COPY mvnw pom.xml lombok.config ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Build the JAR (unit tests included)
COPY src ./src
RUN ./mvnw -B -q package && cp target/*.jar app.jar

# ---- Run -----------------------------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Run as a non-root user
RUN useradd --system --uid 1001 --no-create-home helper730
USER helper730

COPY --from=build /build/app.jar .

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]