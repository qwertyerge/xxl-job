package com.xxl.job.admin.test.support;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple test to verify Docker connection.
 */
class DockerConnectionTest {
    private static final Logger logger = LoggerFactory.getLogger(DockerConnectionTest.class);

    @Test
    void testDockerConnection() {
        logger.info("Testing Docker connection...");
        logger.info("DOCKER_HOST system property: {}", System.getProperty("DOCKER_HOST", "not set"));
        logger.info("DOCKER_HOST env: {}", System.getenv().getOrDefault("DOCKER_HOST", "not set"));

        try (GenericContainer<?> container = new GenericContainer<>("alpine:latest")
                .withCommand("echo", "Hello Docker")) {
            container.start();
            logger.info("Docker connection successful! Container ID: {}", container.getContainerId());
        } catch (Exception e) {
            logger.error("Failed to connect to Docker", e);
            throw e;
        }
    }
}
