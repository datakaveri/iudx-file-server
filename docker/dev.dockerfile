ARG VERSION="0.0.1-SNAPSHOT"

# Using maven base image in builder stage to build Java code.
FROM maven:3-openjdk-11-slim as builder

WORKDIR /usr/share/app
COPY pom.xml .

# Downloads all packages defined in pom.xml
RUN mvn clean package
COPY src src

# Build the source code to generate the fatjar
RUN mvn clean package -Dmaven.test.skip=true

# Java Runtime as the base for final image
FROM openjdk:11-jre-slim-buster

ARG VERSION
ENV JAR="iudx.file.server-dev-${VERSION}-fat.jar"

WORKDIR /usr/share/app

# Copying openapi docs 
COPY docs docs

# Copying dev fatjar from builder stage to final image
COPY --from=builder /usr/share/app/target/${JAR} ./fatjar.jar

EXPOSE 8080 8443

# Creating a non-root user
RUN useradd -r -u 1001 -g root file-user
# Create storage directory and owned by file-user
RUN mkdir -p /usr/share/app/storage/temp_dir &&  mkdir -p /usr/share/app/storage/upload_dir  && chown -R file-user /usr/share/app/storage/
# hint for volume mount 
VOLUME /usr/share/app/storage

# Setting non-root user to use when container starts
USER file-user
