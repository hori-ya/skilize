/**************************************************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * AIチャットサービス。フロントエンドからのチャットリクエストを Python FastAPI（/chat）に
 * 同期転送して応答を返す。CAREER モードでは userId を Python 側に渡し、棚卸データを参照させる。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skilize.ai.application.command.AiChatCommand;
import com.skilize.ai.application.query.AiChatQueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI チャットサービス。フロントエンドからのチャットリクエストを Python FastAPI に同期転送する。
 * キャリアモードでは userId を Python に渡し、Python 側が DB から棚卸データを取得する。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${ai.service.url:http://ai:8000}")
    private String aiServiceUrl;

    @Value("${ai.secret.key:}")
    private String aiSecretKey;

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    private static final String DISABLED_MESSAGE =
            "AI機能は現在無効化されています。ご利用には管理者による設定が必要です。";

    /**
     * AIチャットリクエストを Python FastAPI に転送し、応答を返す。
     * AI機能が無効化されている場合は固定メッセージを返す。
     * Python AIサービスが応答不可の場合は 503 をスローする。
     *
     * @param command チャットコマンド（メッセージ・モード・ユーザーID・会話履歴）
     * @return AIサービスからの応答テキストとモード
     */
    public AiChatQueryResult chat(AiChatCommand command) {
        if (!aiEnabled) {
            log.info("AI chat skipped: AI disabled mode={} userId={}", command.mode(), command.userId());
            return new AiChatQueryResult(DISABLED_MESSAGE, command.mode());
        }
        try {
            String requestBody = buildRequestBody(command);
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(aiServiceUrl + "/chat"))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));
            if (aiSecretKey != null && !aiSecretKey.isEmpty()) {
                builder.header("X-Internal-Key", aiSecretKey);
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("AI chat failed: status={} body={}", response.statusCode(), response.body());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE");
            }

            Map<?, ?> body = OBJECT_MAPPER.readValue(response.body(), Map.class);
            String aiResponse = (String) body.get("response");
            log.info("AI chat completed: mode={} userId={}", command.mode(), command.userId());
            return new AiChatQueryResult(aiResponse, command.mode());

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI chat error: mode={} userId={}", command.mode(), command.userId(), e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AIサービスが一時的に利用できません");
        }
    }

    private String buildRequestBody(AiChatCommand command) throws Exception {
        List<Map<String, String>> historyJson = command.history().stream()
                .map(item -> Map.of("role", item.role(), "content", item.content()))
                .toList();
        Map<String, Object> body = Map.of(
                "message", command.message(),
                "mode", command.mode(),
                "userId", command.userId(),
                "history", historyJson
        );
        return OBJECT_MAPPER.writeValueAsString(body);
    }
}
