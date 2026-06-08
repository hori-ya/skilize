/**************************************************************************************************************
 * 機能ID      ：EXP
 * 機能名      ：期待コメント
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ユーザーへの期待情報エンティティ。TL および ADMIN が各ユーザーに設定する期待コメントを管理する。
 * ユーザーと1対1対応（PK = user_id）で、TL期待と会社期待を独立して保持する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.expectation.domain;

import com.skilize.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * ユーザーへの期待。TL および会社（ADMIN）がユーザーに対して設定する期待値テキスト。
 * ユーザーと1対1対応（PK = user_id）。ユーザー作成時に空レコードが自動生成される。
 * TL期待と会社期待はそれぞれ独立して更新でき、更新日時も別管理。
 *
 * 項目（論理名）:
 *   ユーザー         - 期待を設定する対象ユーザー（1対1）
 *   TL期待           - TL がユーザーに期待する内容（TLが更新）
 *   会社期待         - 会社（ADMIN）がユーザーに期待する内容（ADMINが更新）
 *   TL更新日時       - TL期待の最終更新日時
 *   会社更新日時     - 会社期待の最終更新日時
 */
@Entity
@Table(name = "user_expectations")
@Getter
@NoArgsConstructor
public class UserExpectation {

    // PK（ユーザー内部ID と共有）
    @Id
    @Column(name = "user_id")
    private Integer userId;

    // ユーザー
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // TL期待
    @Column(name = "tl_expectation")
    private String tlExpectation;

    // 会社期待
    @Column(name = "company_expectation")
    private String companyExpectation;

    // TL更新日時
    @Column(name = "tl_updated_at")
    private OffsetDateTime tlUpdatedAt;

    // 会社更新日時
    @Column(name = "company_updated_at")
    private OffsetDateTime companyUpdatedAt;

    public static UserExpectation create(User user) {
        UserExpectation e = new UserExpectation();
        e.user = user;
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
