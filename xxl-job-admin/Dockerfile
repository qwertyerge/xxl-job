# base image
FROM eclipse-temurin:21-jre-alpine

ARG VERSION=3.4.0-SNAPSHOT

LABEL maintainer="xuxueli"
LABEL version="$VERSION"

# timezone
ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# copy jar
COPY target/xxl-job-admin-${VERSION}.jar /app.jar

# Database env family:
#   DB_TYPE=mysql|postgres  DB_HOST  DB_PORT  DB_NAME  DB_USER  DB_PASSWORD  DB_PARAMS
#
# MySQL:
#   docker run -e DB_HOST=mysql -e DB_USER=root -e DB_PASSWORD=secret xxl-job-admin
#
# PostgreSQL:
#   docker run -e DB_TYPE=postgres -e DB_HOST=pg -e DB_USER=postgres -e DB_PASSWORD=secret xxl-job-admin

COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["java", "-jar", "/app.jar"]