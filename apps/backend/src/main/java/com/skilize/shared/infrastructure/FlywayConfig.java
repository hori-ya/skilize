package com.skilize.shared.infrastructure;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

// Spring Boot 4.x で FlywayAutoConfiguration が起動しない問題の回避策として明示的に設定
@Configuration
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true")
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
        return flyway;
    }
}
