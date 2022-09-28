FROM eclipse-temurin:11.0.16_8-jre-alpine

MAINTAINER David Ouagne <david.ouagne@phast.fr>

ENV TZ=Europe/Paris
ENV HOSTNAME=cql-proxy
ENV DISCOVERY_ENABLED=false
ENV DISCOVERY_URL=http://discovery.edge-service:9102/eureka/
ENV ZIPKIN_ENABLED=false
ENV ZIPKIN_URL=http://zipkin:9411

RUN apk --no-cache add curl jq
ADD build/libs/cql-proxy-0.0.1-SNAPSHOT.jar /home/phast/target/

EXPOSE 9205

#run the spring boot application
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dserver.port=9205", \
"-Deureka.client.enabled=${DISCOVERY_ENABLED}","-Deureka.instance.hostname=${HOSTNAME}", \
"-Deureka.client.service-url.defaultZone=${DISCOVERY_URL}","-Dspring.zipkin.enabled=${ZIPKIN_ENABLED}", \
"-Dspring.zipkin.base-url=${ZIPKIN_URL}","-jar","/home/phast/target/cql-proxy-0.0.1-SNAPSHOT.jar"]

HEALTHCHECK --start-period=15s --interval=20s --timeout=10s --retries=5 \
            CMD curl --silent --fail --request GET http://${HOSTNAME}:9205/actuator/health \
                | jq --exit-status '.status == "UP"' || exit 1
