/**************************************************************************************************************
 * 機能ID      ：DSH
 * 機能名      ：ダッシュボード
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ダッシュボードAPIのレスポンス。ログインユーザーの基本情報・現在年度・当年度棚卸サマリを一括で返す。
 * 年度なし・棚卸なしの場合は対応フィールドが null になる。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.dashboard.presentation.response;

/**
 * ダッシュボードレスポンス。GET /api/dashboard のレスポンスに使用する。
 * ログインユーザーの基本情報・現在年度・当年度棚卸サマリを一括で返す。
 *
 * @param user             ログインユーザー情報
 * @param currentFiscalYear 現在有効な年度情報
 * @param currentInventory 当年度の棚卸サマリ（棚卸未作成の場合は null）
 */
public record DashboardResponse(UserInfo user, FiscalYearRef currentFiscalYear,
                                CurrentInventoryInfo currentInventory) {

    /**
     * ログインユーザー情報。
     *
     * @param id   ユーザー内部PK
     * @param name 氏名
     * @param role ロール（GENERAL / TL / ADMIN）
     */
    public record UserInfo(int id, String name, String role) {}

    /**
     * 現在年度の参照情報。
     *
     * @param id             年度内部PK
     * @param name           年度名
     * @param inputStartDate 棚卸入力受付開始日（ISO-8601 形式 "yyyy-MM-dd"）
     * @param inputEndDate   棚卸入力受付終了日（ISO-8601 形式 "yyyy-MM-dd"）
     */
    public record FiscalYearRef(int id, String name, String inputStartDate, String inputEndDate) {}

    /**
     * 当年度棚卸サマリ。
     *
     * @param id                     棚卸内部PK
     * @param status                 棚卸ステータス
     * @param itSkillCount           登録済みITスキル件数
     * @param qualificationCount     登録済み資格件数
     * @param seminarCount           登録済みセミナー件数
     * @param submittedAt            提出日時（未提出の場合は null）
     * @param goalReviewCompletedAt  目標振り返り完了日時（未完了の場合は null）
     * @param goalCompletedAt        目標設定完了日時（未完了の場合は null）
     */
    public record CurrentInventoryInfo(int id, String status,
                                       int itSkillCount, int qualificationCount, int seminarCount,
                                       String submittedAt, String goalReviewCompletedAt, String goalCompletedAt) {}
}
