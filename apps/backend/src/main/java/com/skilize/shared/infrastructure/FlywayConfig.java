package com.skilize.shared.infrastructure;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

// Spring Boot 4.x で FlywayAutoConfiguration が起動しない問題の回避策として明示的に設定
@Configuration
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true")
public class FlywayConfig {

    // LOAD_TEST_DATA=true のときテストユーザー（tl01/user01/user02）を DB に投入する
    @Value("${load.test.data:false}")
    private boolean loadTestData;

    @Bean
    public Flyway flyway(DataSource dataSource) {
        String[] locations = loadTestData
                ? new String[]{"classpath:db/migration", "classpath:db/testdata"}
                : new String[]{"classpath:db/migration"};
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                // LOAD_TEST_DATA=true 時は V4 が V5〜V8 より後から適用されるケースに対応
                .outOfOrder(loadTestData)
                .load();
        flyway.migrate();
        return flyway;
    }
}
