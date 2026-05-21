package com.skilize.inventory.application.command;

/**
 * セミナー明細1件の保存コマンド。InventoryService.saveSeminarDetails() に渡す。
 * adSeminarId が null の場合は自由入力セミナーとして扱い、seminarName / seminarCategoryId を使用する。
 *
 * @param id                既存明細の内部 PK（全件洗い替えのためサーバー側では未使用）
 * @param adSeminarId       ADセミナーマスタの ID（null の場合は自由入力セミナー）
 * @param seminarName       セミナー名（自由入力の場合のみ有効）
 * @param seminarCategoryId セミナー分類 ID（自由入力の場合のみ有効）
 * @param attendedYearMonth 受講年月（ISO-8601 形式: "yyyy-MM-dd"。未設定の場合は null）
 * @param remarks           備考（自由記述）
 */
public record SeminarDetailCommand(Integer id, Integer adSeminarId, String seminarName,
                                   Integer seminarCategoryId, String attendedYearMonth, String remarks) {}
