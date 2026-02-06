package com.xxl.job.admin.test.support;

import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstract base class for all Mapper integration tests.
 * <p>
 * Uses {@code @MybatisTest} with a real MySQL TestContainer
 * (singleton via {@link MySQLContainerHolder}) and rolls back each
 * test via {@code @Transactional}.
 * <p>
 * Excludes {@link DataSourceAutoConfiguration} to avoid Docker environment
 * detection during Spring Boot condition evaluation. The DataSource is
 * manually created in {@link MapperTestConfiguration}.
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
@Import(MapperTestConfiguration.class)
@Transactional
public abstract class MapperITBase {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureMybatis(DynamicPropertyRegistry registry) {
        registry.add("mybatis.mapper-locations", () -> "classpath:/mapper/*Mapper.xml");
    }

}
