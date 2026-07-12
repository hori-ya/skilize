/**************************************************************************************************************
 * 機能ID      ：EXP
 * 機能名      ：期待コメント
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ユーザーへの期待情報JPAエンティティ。user_expectations テーブルとのマッピングを担う。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.UserExpectation から分離（JPAアノテーションはこちらにのみ残す）
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.expectation.infrastructure.persistence.entity;

import com.skilize.user.infrastructure.persistence.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** ユーザーへの期待情報JPAエンティティ。ユーザーと1対1対応（PK = user_id）。 */
@Entity
@Table(name = "user_expectations")
@Getter
@NoArgsConstructor
public class UserExpectationEntity {

    // PK（ユーザー内部ID と共有）
    @Id
    @Column(name = "user_id")
    private Integer userId;

    // ユーザー
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity user;

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

    public static UserExpectationEntity create(UserEntity user) {
        UserExpectationEntity e = new UserExpectationEntity();
        e.user = user;
        return e;
    }

    /** ドメインモデル側で計算済みの状態をそのまま反映する（タイムスタンプを再計算しない）。 */
    public void applyState(String tlExpectation, String companyExpectation,
                           OffsetDateTime tlUpdatedAt, OffsetDateTime companyUpdatedAt) {
        this.tlExpectation = tlExpectation;
        this.companyExpectation = companyExpectation;
        this.tlUpdatedAt = tlUpdatedAt;
        this.companyUpdatedAt = companyUpdatedAt;
    }
}
