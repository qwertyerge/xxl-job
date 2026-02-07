package com.xxl.job.admin.testcontainers;

import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

@MybatisTest
@Transactional
public abstract class PostgresMapperITBase {

    private static final PostgreSQLContainer<?> PG = PostgreSQLContainerHolder.getInstance();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PG::getJdbcUrl);
        registry.add("spring.datasource.username", PG::getUsername);
        registry.add("spring.datasource.password", PG::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("xxl.job.db.type", () -> "postgres");
        registry.add("mybatis.mapper-locations", () -> "classpath:/mapper/postgres/*Mapper.xml");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
    }
}
