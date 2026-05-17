package com.skilize.ai.presentation;

import java.time.OffsetDateTime;

public record AiAnalysisResponse(
        int id,
        int fiscalYearId,
        String status,
        Object analysisResult,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
