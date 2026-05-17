package com.skilize.inventory.domain;

/**
 * 棚卸のステータス遷移: DRAFT（下書き）→ PENDING_GOAL（提出済み・目標設定待ち）→ COMPLETED（目標設定完了）。
 * AI 分析は COMPLETED への遷移時にトリガーされる。
 */
public enum InventoryStatus {
    DRAFT, PENDING_GOAL, COMPLETED
}
