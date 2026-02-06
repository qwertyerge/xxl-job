package com.xxl.job.admin.test.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;

/**
 * Singleton TestContainer for MySQL, shared across all mapper integration tests.
 * The container starts lazily on first access via {@link #getContainer()}.
 */
public final class MySQLContainerHolder {

    private static final Logger logger = LoggerFactory.getLogger(MySQLContainerHolder.class);
    private static volatile MySQLContainer<?> CONTAINER;

    static {
        // Configure Docker environment for macOS Docker Desktop
        // TestContainers checks DOCKER_HOST environment variable first
        String dockerHost = System.getenv("DOCKER_HOST");
        if (dockerHost == null || dockerHost.isEmpty()) {
            // Try Unix socket first (Docker Desktop default)
            dockerHost = "unix:///Users/loki/.docker/run/docker.sock";
            System.setProperty("DOCKER_HOST", dockerHost);
        }

        // Workaround for Docker 29.x compatibility issue with docker-java 3.4.0
        // See: https://github.com/testcontainers/testcontainers-java/issues/11212
        System.setProperty("api.version", "1.44");

        logger.info("Configured DOCKER_HOST={}", dockerHost);
        logger.info("DOCKER_HOST env={}", System.getenv("DOCKER_HOST"));
        logger.info("Docker API version workaround applied (api.version=1.44)");
    }

    private MySQLContainerHolder() {
        // utility class
    }

    /**
     * Get or create the MySQL container. Starts the container on first call.
     */
    private static MySQLContainer<?> getContainer() {
        if (CONTAINER == null) {
            synchronized (MySQLContainerHolder.class) {
                if (CONTAINER == null) {
                    logger.info("Starting MySQL TestContainer...");
                    try {
                        CONTAINER = new MySQLContainer<>("mysql:8.4")
                                .withDatabaseName("xxl_job")
                                .withInitScript("db/tables_xxl_job.sql");
                        CONTAINER.start();
                        logger.info("MySQL TestContainer started successfully at {}", CONTAINER.getJdbcUrl());
                    } catch (Exception e) {
                        logger.error("Failed to start MySQL TestContainer", e);
                        throw new RuntimeException("Failed to start MySQL TestContainer", e);
                    }
                }
            }
        }
        return CONTAINER;
    }

    public static String getJdbcUrl() {
        return getContainer().getJdbcUrl();
    }

    public static String getUsername() {
        return getContainer().getUsername();
    }

    public static String getPassword() {
        return getContainer().getPassword();
    }

}
