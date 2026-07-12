/**************************************************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * AiCareerAnalysisEntity（永続化モデル）と AiCareerAnalysis（ドメインモデル）を相互変換するMapper。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.ai.infrastructure.persistence.mapper;

import com.skilize.ai.domain.model.AiCareerAnalysis;
import com.skilize.ai.infrastructure.persistence.entity.AiCareerAnalysisEntity;
import org.springframework.stereotype.Component;

/** AiCareerAnalysisEntity ⇄ AiCareerAnalysis の変換を担うMapper。 */
@Component
public class AiCareerAnalysisPersistenceMapper {

    /** JPAエンティティからドメインモデルへ変換する。 */
    public AiCareerAnalysis toDomain(AiCareerAnalysisEntity entity) {
        if (entity == null) return null;
        return AiCareerAnalysis.reconstruct(entity.getId(), entity.getUserId(), entity.getFiscalYearId(),
                entity.getStatus(), entity.getAnalysisResult(), entity.getErrorMessage(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
