package com.skilize.fiscalyear.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "fiscal_years")
@Getter
@NoArgsConstructor
public class FiscalYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private LocalDate inputStartDate;
    private LocalDate inputEndDate;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

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
