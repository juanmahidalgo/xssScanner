
#
# OpenJDK Java 7 JRE Dockerfile
#
# https://github.com/dockerfile/java
# https://github.com/dockerfile/java/tree/master/openjdk-7-jre
#

# Pull base image.
FROM ubuntu

RUN apt-get -y install git-core
RUN apt-get update
RUN apt-get -y install default-jdk


# Install Java.
RUN \
  apt-get update && \
  apt-get install -y openjdk-7-jre && \
  rm -rf /var/lib/apt/lists/*




# Define working directory.

WORKDIR /data

# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-7-openjdk-amd64

# Copiamos entry-point.sh
COPY docker-entrypoint.sh /

# El entry point lo que hace es git clone
ENTRYPOINT ["/docker-entrypoint.sh"]

# Ejecuta compilacion y test
CMD /bin/bash

