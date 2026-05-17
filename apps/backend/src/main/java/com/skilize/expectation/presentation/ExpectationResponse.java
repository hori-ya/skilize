package com.skilize.expectation.presentation;

import com.skilize.expectation.domain.UserExpectation;

/**
 * 期待情報レスポンス。TL期待と会社期待の2フィールドを持つ。
 * レコードが DB に存在しない場合は empty() で null フィールドを返す（404 にはしない）。
 */
public record ExpectationResponse(
        String tlExpectation,
        String companyExpectation
) {
    /** UserExpectation エンティティから DTO に変換するファクトリメソッド。 */
    public static ExpectationResponse from(UserExpectation e) {
        return new ExpectationResponse(e.getTlExpectation(), e.getCompanyExpectation());
    }

    /**
     * 期待レコードが未作成の場合に返す空レスポンス。
     * フロントエンドでは null チェックで「未設定」として表示する。
     */
    public static ExpectationResponse empty() {
        return new ExpectationResponse(null, null);
    }
}
