/**************************************************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 棚卸完了イベントリスナー。棚卸のDBコミット確定後（AFTER_COMMIT）にAI分析をトリガーする。
 * 環境変数 AI_ENABLED=false で AI 呼び出しを無効化できる（開発・テスト時のLLM課金回避用）。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.ai.application;

import com.skilize.inventory.application.InventoryCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 棚卸完了イベントを受けて AI 分析をトリガーするリスナー。
 * AFTER_COMMIT フェーズで動作するため、棚卸の DB コミットが確定してから AI サービスへリクエストを送る。
 * 環境変数 AI_ENABLED=false で AI 呼び出しを無効化できる（開発・テスト時の LLM 課金回避用）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryCompletedEventListener {

    private final AiAnalysisService aiAnalysisService;

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    // @TransactionalEventListener(AFTER_COMMIT): 通常の @EventListener はトランザクション内（コミット前）に実行される。
    // AFTER_COMMIT を指定することで、棚卸データが DB に確定した後に AI サービスを呼ぶことを保証する。
    // AFTER_COMMIT で受け取ったメソッドは呼び出し元のトランザクションに参加しない（新たなトランザクションが必要な場合は別途 @Transactional を付ける）。
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInventoryCompleted(InventoryCompletedEvent event) {
        if (!aiEnabled) {
            log.debug("AI feature is disabled. Skipping analysis for user={} fiscalYear={}", event.userId(), event.fiscalYearId());
            return;
        }
        log.info("Triggering AI analysis for user={} fiscalYear={}", event.userId(), event.fiscalYearId());
        aiAnalysisService.upsertPendingAndTrigger(event.userId(), event.fiscalYearId());
    }
}
