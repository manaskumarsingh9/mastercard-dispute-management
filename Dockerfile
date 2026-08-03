FROM maven:3.9-eclipse-temurin-19 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:19-jre
WORKDIR /app

COPY --from=build /app/target/mastercard-dispute-management-0.0.1-SNAPSHOT.jar app.jar
COPY src/data ./data

RUN mkdir -p /app/logs /app/config \
    /app/data/sources/acquirer/customer-comms \
    /app/data/sources/acquirer/device \
    /app/data/sources/acquirer/fraud-tools \
    /app/data/sources/acquirer/identity \
    /app/data/sources/acquirer/merchant \
    /app/data/sources/acquirer/psp \
    /app/data/sources/acquirer/shipping \
    /app/data/sources/issuer/customer-comms \
    /app/data/sources/issuer/device \
    /app/data/sources/issuer/fraud-tools \
    /app/data/sources/issuer/identity \
    /app/data/sources/issuer/merchant \
    /app/data/sources/issuer/psp \
    /app/data/sources/issuer/shipping

EXPOSE 5000

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:5000/api/mastercard/test || exit 1

ENTRYPOINT ["java", \
  "-Xms512m", "-Xmx1536m", \
  "-Dspring.profiles.active=prod", \
  "-jar", "app.jar"]
