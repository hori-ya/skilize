package com.skilize.inventory.application;

/** 棚卸目標完了時に発火するドメインイベント。InventoryCompletedEventListener が AI 分析をトリガーする。 */
public record InventoryCompletedEvent(int userId, int fiscalYearId) {}
