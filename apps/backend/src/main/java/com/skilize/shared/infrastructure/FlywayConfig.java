/**************************************************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * Flyway マイグレーションの明示的設定クラス。
 * Spring Boot 4.x で FlywayAutoConfiguration が起動しない問題の回避策として実装。
 * LOAD_TEST_DATA=true 時はテストデータ用マイグレーションも実行する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.shared.infrastructure;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Flyway マイグレーション設定クラス。
 * spring.flyway.enabled=true のときのみ有効化される（application-local.yml では false）。
 * LOAD_TEST_DATA=true のときはテストデータ（db/testdata/）も適用する。
 */
// Spring Boot 4.x で FlywayAutoConfiguration が起動しない問題の回避策として明示的に設定
@Configuration
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true")
public class FlywayConfig {

    // LOAD_TEST_DATA=true のときテストユーザー（tl01/user01/user02）を DB に投入する
    @Value("${load.test.data:false}")
    private boolean loadTestData;

    /**
     * Flyway Bean を生成してマイグレーションを実行する。
     * LOAD_TEST_DATA=true の場合はテストデータ用ディレクトリ（db/testdata/）も対象に含める。
     * @param dataSource マイグレーション対象のデータソース
     * @return 設定済みの Flyway インスタンス
     */
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
