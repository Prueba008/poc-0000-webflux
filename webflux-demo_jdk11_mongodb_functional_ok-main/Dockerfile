# Stage 1: Build
FROM maven:3.8.8-eclipse-temurin-11 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests clean package

# Stage 2: Runtime
FROM eclipse-temurin:11-jre
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE=prod

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3   CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status"' || exit 1

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
