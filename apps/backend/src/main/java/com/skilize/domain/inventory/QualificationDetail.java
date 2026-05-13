package com.skilize.domain.inventory;

import com.skilize.domain.master.Qualification;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "qualification_details")
@Getter
@NoArgsConstructor
public class QualificationDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qualification_id")
    private Qualification qualification;

    @Column(name = "custom_qualification_name")
    private String customQualificationName;

    @Column(name = "acquired_year_month")
    private LocalDate acquiredYearMonth;

    private String remarks;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

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
