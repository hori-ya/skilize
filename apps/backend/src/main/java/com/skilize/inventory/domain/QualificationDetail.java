package com.skilize.inventory.domain;

import com.skilize.master.domain.Qualification;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 資格棚卸明細。1棚卸あたり複数行の資格取得状況を管理する。
 * マスタ資格またはカスタム資格名のいずれか一方が必ず設定される（両方 null は不可）。
 *
 * 項目（論理名）:
 *   資格          - マスタ参照資格。カスタム資格の場合は null
 *   カスタム資格名 - マスタ未登録の資格名。TL がマスタ昇格できる
 *   取得年月       - 資格取得年月。DATE型だが常に月初日(1日)で保存。未取得は null
 *   備考          - 取得理由・補足説明
 */
@Entity
@Table(name = "qualification_details")
@Getter
@NoArgsConstructor
public class QualificationDetail {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 棚卸
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    // 資格（カスタム資格の場合は null）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qualification_id")
    private Qualification qualification;

    // カスタム資格名（qualification が null の場合に使用）
    @Column(name = "custom_qualification_name")
    private String customQualificationName;

    // 取得年月（月初日で保存。未取得は null）
    @Column(name = "acquired_year_month")
    private LocalDate acquiredYearMonth;

    // 備考
    private String remarks;

    // 作成日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static QualificationDetail create(Inventory inventory, Qualification qualification,
                                              String customQualificationName,
                                              LocalDate acquiredYearMonth, String remarks) {
        QualificationDetail d = new QualificationDetail();
        d.inventory = inventory;
        d.qualification = qualification;
        d.customQualificationName = customQualificationName;
        d.acquiredYearMonth = acquiredYearMonth;
        d.remarks = remarks;
        return d;
    }
}
