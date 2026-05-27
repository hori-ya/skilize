package com.skilize.ai.application;

import com.skilize.ai.application.command.AiChatCommand;
import com.skilize.ai.application.query.AiChatQueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AiChatService のユニットテスト。
 * Python AI サービスへの HTTP 呼び出しは行わない。
 */
class AiChatServiceTest {

    private AiChatService service;

    @BeforeEach
    void setUp() {
        service = new AiChatService();
        ReflectionTestUtils.setField(service, "aiServiceUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(service, "aiSecretKey", "test-key");
    }

    @Test
    void AI無効時は無効メッセージを返しPythonを呼ばない() {
        ReflectionTestUtils.setField(service, "aiEnabled", false);

        AiChatCommand command = new AiChatCommand("テスト質問", "NORMAL", 1, List.of());
        AiChatQueryResult result = service.chat(command);

        assertThat(result.response()).contains("無効化");
        assertThat(result.mode()).isEqualTo("NORMAL");
    }

    @Test
    void AI無効時はモードがそのまま返される() {
        ReflectionTestUtils.setField(service, "aiEnabled", false);

        AiChatCommand command = new AiChatCommand("キャリア相談", "CAREER", 2, List.of());
        AiChatQueryResult result = service.chat(command);

        assertThat(result.response()).contains("無効化");
        assertThat(result.mode()).isEqualTo("CAREER");
    }
}
