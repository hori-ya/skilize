package com.skilize.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skilize.ai.domain.AiCareerAnalysis;
import com.skilize.ai.domain.AiCareerAnalysisRepository;
import com.skilize.ai.presentation.AiAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * AI キャリア分析サービス。棚卸完了イベント受信後、Python FastAPI（/analyze）を非同期で呼び出す。
 * DB への PENDING レコード保存（トランザクション確定）後に AI サービスへ HTTP リクエストを送る。
 * 既存レコードがある場合は PENDING にリセットして再分析する（棚卸を更新して再提出するケースに対応）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    // ObjectMapper はスレッドセーフ。インスタンス生成コストが高いため static final で共有する。
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiCareerAnalysisRepository repository;

    @Value("${ai.service.url:http://ai:8000}")
    private String aiServiceUrl;

    @Value("${ai.secret.key:}")
    private String aiSecretKey;

    @Transactional(readOnly = true)
    public List<AiAnalysisResponse> findByUserId(int userId) {
        return repository.findByUserIdOrderByFiscalYearIdDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // @Async: EventListener から呼ばれる場合はすでに別スレッドになるが、
    //         直接呼び出し時の非同期保証のために付与している
    @Async
    @Transactional
    public void upsertPendingAndTrigger(int userId, int fiscalYearId) {
        AiCareerAnalysis analysis = repository.findByUserIdAndFiscalYearId(userId, fiscalYearId)
                .map(existing -> { existing.resetToPending(); return existing; })
                .orElseGet(() -> AiCareerAnalysis.createPending(userId, fiscalYearId));
        repository.save(analysis);
        callAiService(userId, fiscalYearId);
    }

    private void callAiService(int userId, int fiscalYearId) {
        try {
            String body = String.format("{\"userId\":%d,\"fiscalYearId\":%d}", userId, fiscalYearId);
            // HTTP/1.1 を明示指定: Java の HttpClient はデフォルトで HTTP/2 を試みるが、
            // 内部サービス（Python FastAPI）が HTTP/2 に未対応の場合にネゴシエーション失敗を防ぐ。
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(aiServiceUrl + "/analyze"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            // 内部サービス間認証: X-Internal-Key ヘッダーで Python AI サービスへの不正アクセスを防ぐ
            if (aiSecretKey != null && !aiSecretKey.isEmpty()) {
                requestBuilder.header("X-Internal-Key", aiSecretKey);
            }
            HttpRequest request = requestBuilder.build();
            // fire-and-forget: AI サービスは非同期で処理するため応答を待たない
            client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ex -> {
                        log.error("AI service call failed for user={} fiscalYear={}", userId, fiscalYearId, ex);
                        return null;
                    });
        } catch (Exception e) {
            log.error("Failed to call AI service for user={} fiscalYear={}", userId, fiscalYearId, e);
        }
    }

    private AiAnalysisResponse toResponse(AiCareerAnalysis a) {
        Object parsed = null;
        if (a.getAnalysisResult() != null) {
            try {
                // DB には JSON 文字列で保存されているため、API レスポンス時に Object にパースして返す
                parsed = OBJECT_MAPPER.readValue(a.getAnalysisResult(), Object.class);
            } catch (Exception e) {
                log.warn("Failed to parse analysisResult for id={}", a.getId(), e);
            }
        }
        return new AiAnalysisResponse(
                a.getId(),
                a.getFiscalYearId(),
                a.getStatus().name(),
                parsed,
                a.getErrorMessage(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
