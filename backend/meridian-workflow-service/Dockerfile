FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
COPY backend/meridian-workflow-service/target/meridian-workflow-service-1.0.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
