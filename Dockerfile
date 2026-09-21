FROM eclipse-temurin:21-jdk-alpine as build
WORKDIR /app

# 1. Copy ONLY the Maven configuration files to leverage Docker layer caching
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# 2. Download most dependencies (Docker caches this layer as long as pom.xml remains unchanged)
RUN ./mvnw dependency:go-offline

# 3. Copy the source code
COPY src ./src

# 4. Build the application (Removed -o flag since go-offline is not 100% exhaustive)
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]