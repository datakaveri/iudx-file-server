ARG VERSION="0.0.1-SNAPSHOT"

# Using maven base image in builder stage to build Java code.
FROM maven:3-eclipse-temurin-11 as builder


WORKDIR /usr/share/app
COPY pom.xml .
# Downloads all packages defined in pom.xml
RUN mvn clean package
COPY src src

# Build the source code to generate the fatjar
RUN mvn clean package -Dmaven.test.skip=true

# Java Runtime as the base for final image
FROM eclipse-temurin:11-jre-focal


ARG VERSION
ENV JAR="iudx.file.server-cluster-${VERSION}-fat.jar"

WORKDIR /usr/share/app

# Copying openapi docs 
COPY docs docs
COPY iudx-pmd-ruleset.xml iudx-pmd-ruleset.xml
COPY google_checks.xml google_checks.xml
COPY checkstyle-suppressions.xml checkstyle-suppressions.xml



# Copying dev fatjar from builder stage to final image
COPY --from=builder /usr/share/app/target/${JAR} ./fatjar.jar

EXPOSE 8080 8443

# Creating a non-root user
RUN useradd -r -u 1001 -g root file-user
# Create storage directory and owned by file-user
RUN mkdir -p /usr/share/app/storage/temp-dir &&  mkdir -p /usr/share/app/storage/upload-dir  && chown -R file-user /usr/share/app/storage/
# hint for volume mount 
VOLUME /usr/share/app/storage
# Setting non-root user to use when container starts

USER file-user
