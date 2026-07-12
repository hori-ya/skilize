/**************************************************************************************************************
 * 機能ID      ：FY
 * 機能名      ：年度管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 年度JPAエンティティ。fiscal_years テーブルとのマッピングを担う。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.FiscalYear から分離（JPAアノテーションはこちらにのみ残す）
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.fiscalyear.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 年度JPAエンティティ。棚卸の対象年度を管理する。
 * 棚卸入力期間（入力開始日〜入力終了日）は年度期間（開始日〜終了日）と独立して設定できる。
 */
@Entity
@Table(name = "fiscal_years")
@Getter
@NoArgsConstructor
public class FiscalYearEntity {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 年度名
    @Column(nullable = false)
    private String name;

    // 開始日
    @Column(nullable = false)
    private LocalDate startDate;

    // 終了日
    @Column(nullable = false)
    private LocalDate endDate;

    // 入力開始日
    private LocalDate inputStartDate;
    // 入力終了日
    private LocalDate inputEndDate;

    // 有効フラグ
    @Column(name = "is_active", nullable = false)
    private boolean active;

    // 作成日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    /**
     * 年度エンティティを新規作成する。有効フラグは初期値 true。
     */
    public static FiscalYearEntity create(String name, LocalDate startDate, LocalDate endDate,
                                          LocalDate inputStartDate, LocalDate inputEndDate) {
        FiscalYearEntity f = new FiscalYearEntity();
        f.name = name;
        f.startDate = startDate;
        f.endDate = endDate;
        f.inputStartDate = inputStartDate;
        f.inputEndDate = inputEndDate;
        f.active = true;
        return f;
    }

    /**
     * 年度情報を更新する。active フラグで有効・無効を切り替えられる。
     */
    public void update(String name, LocalDate startDate, LocalDate endDate,
                       LocalDate inputStartDate, LocalDate inputEndDate, boolean active) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.inputStartDate = inputStartDate;
        this.inputEndDate = inputEndDate;
        this.active = active;
    }
}
