# Run from project root directory

ARG VERSION="0.0.1-SNAPSHOT"

FROM maven:latest as dependencies

WORKDIR /usr/share/app
COPY pom.xml .
RUN mvn clean package