package com.xxl.job.admin.test.support;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Test application configuration for Mapper integration tests.
 * <p>
 * This configuration manually creates the DataSource bean from the TestContainer,
 * bypassing Spring Boot's {@code DataSourceAutoConfiguration} to avoid Docker
 * environment detection during condition evaluation.
 */
@SpringBootApplication(scanBasePackages = "com.xxl.job.admin.mapper")
public class MapperTestConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource() {
        HikariDataSource dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(MySQLContainerHolder.getJdbcUrl())
                .username(MySQLContainerHolder.getUsername())
                .password(MySQLContainerHolder.getPassword())
                .build();
        dataSource.setMaximumPoolSize(5);
        return dataSource;
    }

    @Bean
    @ConditionalOnMissingBean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource);
    }

}
