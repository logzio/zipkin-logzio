FROM alpine

WORKDIR /zipkin-logzio


RUN apk add curl unzip && \
  curl -sSL https://jitpack.io/com/github/logzio/zipkin-logzio/zipkin-autoconfigure-storage-logzio/master-SNAPSHOT/zipkin-autoconfigure-storage-logzio-master-SNAPSHOT-module.jar > logzio.jar && \
  unzip logzio.jar -d logzio && \
  rm logzio.jar

FROM openzipkin/zipkin:2.15.0
MAINTAINER OpenZipkin "https://zipkin.io/"

COPY --from=0 /zipkin-logzio/ /zipkin/

ENV STORAGE_TYPE=logzio

env MODULE_OPTS="-Dloader.path=logzio -Dspring.profiles.active=logzio"