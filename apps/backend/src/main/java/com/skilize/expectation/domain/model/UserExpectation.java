/**************************************************************************************************************
 * 機能ID      ：EXP
 * 機能名      ：期待コメント
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ユーザーへの期待情報ドメインモデル。TL および ADMIN が各ユーザーに設定する期待コメントを管理する。
 * ユーザーと1対1対応（PK = user_id）で、TL期待と会社期待を独立して保持する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: infrastructure.persistence.entity.UserExpectationEntity から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.expectation.domain.model;

import com.skilize.user.domain.model.User;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * ユーザーへの期待。TL および会社（ADMIN）がユーザーに対して設定する期待値テキスト。
 * JPA/Springに依存しない純粋なドメインモデル。ユーザーと1対1対応（PK = user_id）。
 * ユーザー作成時に空レコードが自動生成される。TL期待と会社期待はそれぞれ独立して更新でき、更新日時も別管理。
 */
@Getter
@NoArgsConstructor
public class UserExpectation {

    private Integer userId;
    private User user;
    private String tlExpectation;
    private String companyExpectation;
    private OffsetDateTime tlUpdatedAt;
    private OffsetDateTime companyUpdatedAt;

    public static UserExpectation create(User user) {
        UserExpectation e = new UserExpectation();
        e.user = user;
        e.userId = user.getId();
        return e;
    }

    /**
     * 永続化済みの状態から期待情報を復元する。infrastructure層のMapperからのみ呼び出す。
     */
    public static UserExpectation reconstruct(Integer userId, User user, String tlExpectation, String companyExpectation,
                                              OffsetDateTime tlUpdatedAt, OffsetDateTime companyUpdatedAt) {
        UserExpectation e = new UserExpectation();
        e.userId = userId;
        e.user = user;
        e.tlExpectation = tlExpectation;
        e.companyExpectation = companyExpectation;
        e.tlUpdatedAt = tlUpdatedAt;
        e.companyUpdatedAt = companyUpdatedAt;
        return e;
    }

    public void updateTlExpectation(String expectation) {
        this.tlExpectation = expectation;
        this.tlUpdatedAt = OffsetDateTime.now();
    }

    public void updateCompanyExpectation(String expectation) {
        this.companyExpectation = expectation;
        this.companyUpdatedAt = OffsetDateTime.now();
    }
}
