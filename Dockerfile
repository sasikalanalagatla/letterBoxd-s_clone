# Build stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests


# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /app/target/blog-application-0.0.1-SNAPSHOT.jar app.jar

# limit memory for Render free tier
ENTRYPOINT ["java","-Xmx256m","-jar","app.jar"]