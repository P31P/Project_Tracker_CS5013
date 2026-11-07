# The demo image graders run: the tracker with its own PostgreSQL and sample data, in demo mode.
#   docker build -t raman-lab-tracker .
#   docker run -p 8080:8080 raman-lab-tracker

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /build/target/tracker-*.jar /app/tracker.jar
COPY src/test/resources/seed-mock-data.sql docker/demo-user.sql /app/seed/
COPY docker/entrypoint.sh /app/entrypoint.sh
ENV SPRING_PROFILES_ACTIVE=demo
EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]
