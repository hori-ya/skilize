package com.skilize.domain.inventory;

import com.skilize.domain.master.AdSeminar;
import com.skilize.domain.master.SeminarCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "seminar_details")
@Getter
@NoArgsConstructor
public class SeminarDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_seminar_id")
    private AdSeminar adSeminar;

    @Column(name = "seminar_name")
    private String seminarName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seminar_category_id")
    private SeminarCategory seminarCategory;

    @Column(name = "attended_year_month")
    private LocalDate attendedYearMonth;

    private String remarks;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

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
