#!/bin/sh
set -e

# -------------------------------------------------------
# Resolve DB_* env family into Spring Boot properties.
#
#   DB_TYPE     : mysql (default) | postgres
#   DB_HOST     : database host
#   DB_PORT     : database port  (auto: mysql=3306, postgres=5432)
#   DB_NAME     : database name  (default: xxl_job)
#   DB_USER     : username
#   DB_PASSWORD : password
#   DB_PARAMS   : extra JDBC query string
# -------------------------------------------------------

DB_TYPE="${DB_TYPE:-mysql}"
DB_NAME="${DB_NAME:-xxl_job}"

if [ -n "$DB_HOST" ]; then
  case "$DB_TYPE" in
    mysql)
      DB_PORT="${DB_PORT:-3306}"
      DB_PARAMS="${DB_PARAMS:-useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai}"
      export SPRING_DATASOURCE_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?${DB_PARAMS}"
      export SPRING_DATASOURCE_DRIVER_CLASS_NAME="com.mysql.cj.jdbc.Driver"
      ;;
    postgres)
      DB_PORT="${DB_PORT:-5432}"
      export SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}${DB_PARAMS:+?${DB_PARAMS}}"
      export SPRING_DATASOURCE_DRIVER_CLASS_NAME="org.postgresql.Driver"
      ;;
  esac

  export SPRING_DATASOURCE_USERNAME="${DB_USER}"
  export SPRING_DATASOURCE_PASSWORD="${DB_PASSWORD}"
fi

export XXL_JOB_DB_TYPE="${DB_TYPE}"

# LOG_HOME -> JVM system property
if [ -n "$LOG_HOME" ]; then
  export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -DLOG_HOME=${LOG_HOME}"
fi

exec "$@"
