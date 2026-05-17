package com.skilize.ai.application;

import com.skilize.inventory.application.InventoryCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryCompletedEventListener {

    private final AiAnalysisService aiAnalysisService;

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

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
