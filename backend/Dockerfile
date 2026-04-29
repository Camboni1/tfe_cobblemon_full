FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./
COPY cobblemon-api/pom.xml cobblemon-api/pom.xml
COPY cobblemon-core/pom.xml cobblemon-core/pom.xml
COPY cobblemon-importer/pom.xml cobblemon-importer/pom.xml

COPY cobblemon-api/src cobblemon-api/src
COPY cobblemon-core/src cobblemon-core/src

RUN mvn -B -pl cobblemon-api -am clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd -r -u 1001 spring

COPY --from=build /app/cobblemon-api/target/*.jar app.jar
COPY assets ./assets

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
