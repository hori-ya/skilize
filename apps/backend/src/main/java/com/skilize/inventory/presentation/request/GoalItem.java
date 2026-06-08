/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 目標1件のリクエスト要素。GoalsRequest のリスト要素として目標一括保存リクエストに使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.presentation.request;

/**
 * 目標1件のリクエスト要素。GoalsRequest のリスト要素として使用する。
 * goalCategory に応じて itSkillId / qualificationId / adSeminarId のいずれか1つが設定される。
 *
 * @param id              既存目標の内部 PK（全件洗い替えのためサーバー側では未使用）
 * @param goalCategory    目標カテゴリ（GoalCategory enum の文字列: IT_SKILL / QUALIFICATION / AD）
 * @param itSkillId       ITスキルマスタの ID（goalCategory が IT_SKILL の場合）
 * @param qualificationId 資格マスタの ID（goalCategory が QUALIFICATION の場合）
 * @param adSeminarId     ADセミナーマスタの ID（goalCategory が AD の場合）
 * @param customName      カスタム目標名（マスタ未登録の場合）
 * @param targetPeriod    目標達成期限（ISO-8601 形式: "yyyy-MM-dd"）
 * @param reason          目標設定理由
 */
public record GoalItem(Integer id, String goalCategory, Integer itSkillId,
                       Integer qualificationId, Integer adSeminarId,
                       String customName, String targetPeriod, String reason) {}
