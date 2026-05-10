package com.skilize.domain.fiscalyear;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "fiscal_year_settings")
@Getter
@NoArgsConstructor
public class FiscalYearSettings {

    @Id
    private Short id;

    @Column(nullable = false)
    private Short fiscalYearStartMonth;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public void setFiscalYearStartMonth(Short month) {
        this.fiscalYearStartMonth = month;
    }
}
