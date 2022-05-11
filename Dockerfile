FROM maven:latest
RUN mkdir -p /opt/project && cd /opt/project
COPY . /opt/project
RUN mvn compile && mvn package
CMD ["java", "-cp", "target/Routinator-1.0-SNAPSHOT.jar", "de.haw.cps22rs.Entry"]

