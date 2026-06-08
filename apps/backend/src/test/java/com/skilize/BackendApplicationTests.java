package com.skilize;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Spring Boot コンテキスト起動テスト。
 * アプリケーション全体のコンテキストが正常にロードされることを確認する。
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
