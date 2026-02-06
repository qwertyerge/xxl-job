package com.xxl.job.admin.test.support;

import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstract base class for all Mapper integration tests.
 * <p>
 * Uses {@code @MybatisTest} slice with a real MySQL TestContainer
 * (singleton via {@link MySQLContainerHolder}) and rolls back each
 * test via {@code @Transactional}.
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public abstract class MapperITBase {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MySQLContainerHolder::getJdbcUrl);
        registry.add("spring.datasource.username", MySQLContainerHolder::getUsername);
        registry.add("spring.datasource.password", MySQLContainerHolder::getPassword);
        registry.add("mybatis.mapper-locations", () -> "classpath:/mapper/*Mapper.xml");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
    }

}
