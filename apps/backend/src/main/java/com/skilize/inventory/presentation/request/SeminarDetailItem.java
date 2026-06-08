/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * セミナー明細1件のリクエスト要素。SeminarDetailsRequest のリスト要素としてセミナー明細一括保存リクエストに使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.presentation.request;

/**
 * セミナー明細1件のリクエスト要素。SeminarDetailsRequest のリスト要素として使用する。
 * adSeminarId が null の場合は自由入力セミナーとして扱い、seminarName / seminarCategoryId を使用する。
 *
 * @param id                既存明細の内部 PK（全件洗い替えのためサーバー側では未使用）
 * @param adSeminarId       ADセミナーマスタの ID（null の場合は自由入力セミナー）
 * @param seminarName       セミナー名（自由入力の場合のみ有効）
 * @param seminarCategoryId セミナー分類 ID（自由入力の場合のみ有効）
 * @param attendedYearMonth 受講年月（ISO-8601 形式: "yyyy-MM-dd"。未設定の場合は null）
 * @param remarks           備考（自由記述）
 */
public record SeminarDetailItem(Integer id, Integer adSeminarId, String seminarName,
                                Integer seminarCategoryId, String attendedYearMonth, String remarks) {}
