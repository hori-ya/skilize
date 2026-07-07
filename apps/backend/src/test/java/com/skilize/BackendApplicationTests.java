package com.skilize;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Spring Boot コンテキスト起動テスト。
 * アプリケーション全体のコンテキストが正常にロードされることを確認する。
 * test プロファイルを有効にし、ログ出力をコンソールのみにする（本番用ファイル出力先 /var/log/skilize を CI 実行環境に作らせないため）。
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
