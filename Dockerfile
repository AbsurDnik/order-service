####
# This Dockerfile is used to build a Quarkus application in JVM mode
####
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

####
# Run stage
####
FROM eclipse-temurin:21-jre-alpine
WORKDIR /work

# Copy the application jar
COPY --from=build /app/target/quarkus-app/lib/ /work/lib/
COPY --from=build /app/target/quarkus-app/*.jar /work/
COPY --from=build /app/target/quarkus-app/app/ /work/app/
COPY --from=build /app/target/quarkus-app/quarkus/ /work/quarkus/

EXPOSE 8080

# Run the application
CMD ["java", "-jar", "/work/quarkus-run.jar"]
