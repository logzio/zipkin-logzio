#!/usr/bin/env bash
#LOGZIO_LISTENER_HOST=listener.logz.io \
#LOGZIO_ACCOUNT_TOKEN=<ACCOUNT_TOKEN> \
#LOGZIO_API_TOKEN=<API_TOKEN> \
#LOGZIO_API_HOST=api.logz.io \
#STRICT_TRACE_ID=true \
STORAGE_TYPE=logzio \
java -Dloader.path='zipkin-logzio.jar,zipkin-logzio.jar!lib' -Dspring.profiles.active=logzio -cp zipkin.jar org.springframework.boot.loader.PropertiesLauncher