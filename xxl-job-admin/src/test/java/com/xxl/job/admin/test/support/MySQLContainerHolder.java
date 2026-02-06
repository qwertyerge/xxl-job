package com.xxl.job.admin.test.support;

import org.testcontainers.containers.MySQLContainer;

/**
 * Singleton TestContainer for MySQL, shared across all mapper integration tests.
 * The container starts once per JVM lifecycle via static initializer.
 */
public final class MySQLContainerHolder {

    private static final MySQLContainer<?> CONTAINER;

    static {
        CONTAINER = new MySQLContainer<>("mysql:8.4")
                .withDatabaseName("xxl_job")
                .withInitScript("db/tables_xxl_job.sql");
        CONTAINER.start();
    }

    private MySQLContainerHolder() {
        // utility class
    }

    public static String getJdbcUrl() {
        return CONTAINER.getJdbcUrl();
    }

    public static String getUsername() {
        return CONTAINER.getUsername();
    }

    public static String getPassword() {
        return CONTAINER.getPassword();
    }

}
