/**************************************************************************************************************
 * 機能ID      ：EXP
 * 機能名      ：期待コメント
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 期待情報クエリ結果クラス。TL期待と会社期待の2フィールドを持ち、
 * GET /api/users/{userId}/expectations のレスポンスとして使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.expectation.application.query;

import com.skilize.expectation.domain.model.UserExpectation;

/**
 * 期待情報クエリ結果。GET /api/users/{userId}/expectation のレスポンスに使用する。
 * TL期待と会社期待の2フィールドを持つ。レコードが DB に存在しない場合は empty() で null フィールドを返す（404 にはしない）。
 *
 * @param tlExpectation      TLからの期待コメント（未設定の場合は null）
 * @param companyExpectation 会社からの期待コメント（未設定の場合は null）
 */
public record ExpectationQueryResult(
        String tlExpectation,
        String companyExpectation
) {
    /** UserExpectation エンティティからクエリ結果に変換するファクトリメソッド。 */
    public static ExpectationQueryResult from(UserExpectation e) {
        return new ExpectationQueryResult(e.getTlExpectation(), e.getCompanyExpectation());
    }

    /**
     * 期待レコードが未作成の場合に返す空クエリ結果。
     * フロントエンドでは null チェックで「未設定」として表示する。
     */
    public static ExpectationQueryResult empty() {
        return new ExpectationQueryResult(null, null);
    }
}
