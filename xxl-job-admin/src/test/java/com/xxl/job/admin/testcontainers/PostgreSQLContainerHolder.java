package com.xxl.job.admin.testcontainers;

import org.testcontainers.containers.PostgreSQLContainer;

public final class PostgreSQLContainerHolder {

    private static final PostgreSQLContainer<?> CONTAINER;

    static {
        CONTAINER = new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("xxl_job")
                .withInitScript("db/tables_xxl_job_postgres.sql");
        CONTAINER.start();
    }

    private PostgreSQLContainerHolder() {
    }

    public static PostgreSQLContainer<?> getInstance() {
        return CONTAINER;
    }
}
