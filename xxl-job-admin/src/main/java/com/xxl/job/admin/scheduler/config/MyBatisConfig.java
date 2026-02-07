package com.xxl.job.admin.scheduler.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis configuration for multi-database support.
 *
 * <p>Reads {@code xxl.job.db.type} property and logs the active database type at startup.
 * The mapper-locations are resolved via property placeholder in application.properties:
 * {@code mybatis.mapper-locations=classpath:/mapper/${xxl.job.db.type:mysql}/*Mapper.xml}</p>
 */
@Configuration
public class MyBatisConfig {
    private static final Logger logger = LoggerFactory.getLogger(MyBatisConfig.class);

    @Value("${xxl.job.db.type:mysql}")
    private String dbType;

    @PostConstruct
    public void init() {
        String resolvedDbType = resolveDbType(dbType);
        logger.info(">>>>>>>>> xxl-job database type: {}, mapper-locations: classpath:/mapper/{}/*Mapper.xml",
                resolvedDbType, resolvedDbType);
    }

    private String resolveDbType(String dbType) {
        if (dbType == null || dbType.isBlank()) {
            logger.warn("xxl.job.db.type is empty, defaulting to mysql");
            return "mysql";
        }
        String normalized = dbType.trim().toLowerCase();
        if (!"mysql".equals(normalized) && !"postgres".equals(normalized)) {
            logger.warn("Unsupported xxl.job.db.type: '{}', defaulting to mysql. Supported values: mysql, postgres", dbType);
            return "mysql";
        }
        return normalized;
    }
}
