package com.skilize.inventory.domain;

import com.skilize.master.domain.AdSeminar;
import com.skilize.master.domain.SeminarCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * セミナー棚卸明細。1棚卸あたり複数行のセミナー受講履歴を管理する。
 * ADセミナーまたは自由入力セミナー名のいずれか一方が必ず設定される（両方 null は不可）。
 * セミナー分類は自由入力セミナー時のみ設定する。ADセミナー時は null。
 *
 * 項目（論理名）:
 *   ADセミナー       - マスタ参照ADセミナー。自由入力セミナーの場合は null
 *   セミナー名       - 自由入力セミナー名（ADセミナーが null の場合に使用）
 *   セミナー分類     - 自由入力セミナーの分類（ADセミナー時は null）
 *   受講年月         - 受講年月。DATE型だが常に月初日(1日)で保存。未受講は null
 *   備考            - 受講理由・振り返り
 */
@Entity
@Table(name = "seminar_details")
@Getter
@NoArgsConstructor
public class SeminarDetail {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 棚卸
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    // ADセミナー（自由入力セミナーの場合は null）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_seminar_id")
    private AdSeminar adSeminar;

    // セミナー名（adSeminar が null の場合に使用）
    @Column(name = "seminar_name")
    private String seminarName;

    // セミナー分類（adSeminar が null の場合のみ設定）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seminar_category_id")
    private SeminarCategory seminarCategory;

    // 受講年月（月初日で保存。未受講は null）
    @Column(name = "attended_year_month")
    private LocalDate attendedYearMonth;

    // 備考
    private String remarks;

    // 作成日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static SeminarDetail create(Inventory inventory, AdSeminar adSeminar, String seminarName,
                                       SeminarCategory seminarCategory,
                                       LocalDate attendedYearMonth, String remarks) {
        SeminarDetail d = new SeminarDetail();
        d.inventory = inventory;
        d.adSeminar = adSeminar;
        d.seminarName = seminarName;
        d.seminarCategory = seminarCategory;
        d.attendedYearMonth = attendedYearMonth;
        d.remarks = remarks;
        return d;
    }
}
