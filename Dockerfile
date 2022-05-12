FROM maven:latest
RUN mkdir -p /opt/project /opt/run
COPY . /opt/project
WORKDIR /opt/project
RUN mvn package

FROM eclipse-temurin:17-jdk
RUN mkdir -p /opt/run/maps
WORKDIR /opt/run
COPY --from=0 /opt/project/target/Routinator-1.0-SNAPSHOT-jar-with-dependencies.jar ./Routinator.jar
CMD ["java", "-jar", "Routinator.jar"]

