/**************************************************************************************************************
 * 機能ID      ：FY
 * 機能名      ：年度管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 年度マスタエンティティ。棚卸の対象年度（年度期間・入力受付期間・有効フラグ）を管理する。
 * フィールドはドメインメソッド（create/update）でのみ変更する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.fiscalyear.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 年度マスタ。棚卸の対象年度を管理する。
 * 棚卸入力期間（入力開始日〜入力終了日）は年度期間（開始日〜終了日）と独立して設定できる。
 *
 * 項目（論理名）:
 *   年度名       - 表示名（例: "2025年度"）
 *   開始日       - 年度の開始日
 *   終了日       - 年度の終了日
 *   入力開始日   - 棚卸の入力受付開始日（null は制限なし）
 *   入力終了日   - 棚卸の入力受付終了日（null は制限なし）
 *   有効フラグ   - false で一覧から除外（論理削除）
 */
@Entity
@Table(name = "fiscal_years")
@Getter
@NoArgsConstructor
public class FiscalYear {

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
     *
     * @param name           年度名（例: "2025年度"）
     * @param startDate      年度開始日
     * @param endDate        年度終了日
     * @param inputStartDate 棚卸入力受付開始日（null で制限なし）
     * @param inputEndDate   棚卸入力受付終了日（null で制限なし）
     * @return 新規作成された年度エンティティ
     */
    public static FiscalYear create(String name, LocalDate startDate, LocalDate endDate,
                                    LocalDate inputStartDate, LocalDate inputEndDate) {
        FiscalYear f = new FiscalYear();
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
     *
     * @param name           年度名
     * @param startDate      年度開始日
     * @param endDate        年度終了日
     * @param inputStartDate 棚卸入力受付開始日（null で制限なし）
     * @param inputEndDate   棚卸入力受付終了日（null で制限なし）
     * @param active         有効フラグ（false で論理無効化）
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
