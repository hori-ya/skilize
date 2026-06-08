/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキル明細1件のリクエスト要素。ItSkillDetailsRequest のリスト要素としてITスキル明細一括保存リクエストに使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.presentation.request;

/**
 * ITスキル明細1件のリクエスト要素。ItSkillDetailsRequest のリスト要素として使用する。
 * itSkillId と customSkillName は排他（マスタ登録スキルか自由入力スキルかのいずれか）。
 *
 * @param id              既存明細の内部 PK（全件洗い替えのためサーバー側では未使用）
 * @param itSkillId       ITスキルマスタの ID（null の場合はカスタムスキル）
 * @param customSkillName カスタムスキル名（itSkillId が null の場合のみ有効）
 * @param skillLevelId    スキルレベルマスタの ID
 * @param remarks         備考（自由記述）
 */
public record ItSkillDetailItem(Integer id, Integer itSkillId, String customSkillName,
                                int skillLevelId, String remarks) {}
