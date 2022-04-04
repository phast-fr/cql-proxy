FROM eclipse-temurin:11.0.14_9-jre-alpine

MAINTAINER David Ouagne <david.ouagne@phast.fr>

ENV TZ=Europe/Paris
ENV HOSTNAME=cql-proxy

RUN apk --no-cache add curl jq
ADD build/libs/cql-proxy-0.0.1-SNAPSHOT.jar /home/phast/target/

EXPOSE 9205

#run the spring boot application
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dserver.port=9205", \
"-Deureka.instance.hostname=${HOSTNAME}","-jar","/home/phast/target/cql-proxy-0.0.1-SNAPSHOT.jar"]

HEALTHCHECK --start-period=15s --interval=20s --timeout=10s --retries=5 \
            CMD curl --silent --fail --request GET http://${HOSTNAME}:9205/actuator/health \
                | jq --exit-status '.status == "UP"' || exit 1
