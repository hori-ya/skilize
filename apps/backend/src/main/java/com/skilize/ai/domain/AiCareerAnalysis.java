package com.skilize.ai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ai_career_analyses")
@Getter
@NoArgsConstructor
public class AiCareerAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "fiscal_year_id", nullable = false)
    private Integer fiscalYearId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiAnalysisStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_result", columnDefinition = "jsonb")
    private String analysisResult;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static AiCareerAnalysis createPending(int userId, int fiscalYearId) {
        AiCareerAnalysis a = new AiCareerAnalysis();
        a.userId = userId;
        a.fiscalYearId = fiscalYearId;
        a.status = AiAnalysisStatus.PENDING;
        return a;
    }

    public void resetToPending() {
        this.status = AiAnalysisStatus.PENDING;
        this.analysisResult = null;
        this.errorMessage = null;
    }
}
