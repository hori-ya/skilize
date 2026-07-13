/**************************************************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * AI キャリア分析サービス。棚卸完了イベント受信後、Python FastAPI（/analyze）を非同期で呼び出す。
 * PENDING レコードのDB保存後にAIサービスへHTTPリクエストを送り、既存レコードは再分析時にリセットする。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skilize.ai.domain.model.AiCareerAnalysis;
import com.skilize.ai.domain.repository.AiCareerAnalysisRepository;
import com.skilize.ai.application.query.AiAnalysisQueryResult;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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

    /**
     * 指定ユーザーのAIキャリア分析結果一覧を年度降順で返す。
     *
     * @param userId ユーザー内部ID
     * @return 分析結果クエリ結果リスト（新しい年度順）
     */
    @Transactional(readOnly = true)
    public List<AiAnalysisQueryResult> findByUserId(int userId) {
        List<AiAnalysisQueryResult> results = new ArrayList<>();
        for (AiCareerAnalysis analysis : repository.findByUserIdOrderByFiscalYearIdDesc(userId)) {
            results.add(toQueryResult(analysis));
        }
        return results;
    }

    /**
     * PENDING状態の分析レコードをupsertしてAIサービスへの分析をトリガーする。
     * 既存レコードがある場合はPENDINGにリセットして再分析する。非同期（@Async）で実行する。
     *
     * @param userId       分析対象ユーザーの内部ID
     * @param fiscalYearId 分析対象年度の内部ID
     */
    // @Async: EventListener から呼ばれる場合はすでに別スレッドになるが、
    //         直接呼び出し時の非同期保証のために付与している
    @Async
    @Transactional
    public void upsertPendingAndTrigger(int userId, int fiscalYearId) {
        Optional<AiCareerAnalysis> existingOptional = repository.findByUserIdAndFiscalYearId(userId, fiscalYearId);
        AiCareerAnalysis analysis;
        if (existingOptional.isPresent()) {
            analysis = existingOptional.get();
            analysis.resetToPending();
        } else {
            analysis = AiCareerAnalysis.createPending(userId, fiscalYearId);
        }
        repository.save(analysis);
        log.info("AI analysis triggered: userId={} fiscalYearId={}", userId, fiscalYearId);
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
                    .exceptionally(new Function<Throwable, HttpResponse<Void>>() {
                        @Override
                        public HttpResponse<Void> apply(Throwable ex) {
                            log.error("AI service call failed for user={} fiscalYear={}", userId, fiscalYearId, ex);
                            return null;
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to call AI service for user={} fiscalYear={}", userId, fiscalYearId, e);
        }
    }

    private AiAnalysisQueryResult toQueryResult(AiCareerAnalysis a) {
        Object parsed = null;
        if (a.getAnalysisResult() != null) {
            try {
                // DB には JSON 文字列で保存されているため、API レスポンス時に Object にパースして返す
                parsed = OBJECT_MAPPER.readValue(a.getAnalysisResult(), Object.class);
            } catch (Exception e) {
                log.warn("Failed to parse analysisResult for id={}", a.getId(), e);
            }
        }
        return new AiAnalysisQueryResult(
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
