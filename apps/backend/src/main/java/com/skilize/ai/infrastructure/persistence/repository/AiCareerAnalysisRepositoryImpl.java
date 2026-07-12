/**************************************************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.AiCareerAnalysisRepository の実装クラス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.ai.infrastructure.persistence.repository;

import com.skilize.ai.domain.model.AiAnalysisStatus;
import com.skilize.ai.domain.model.AiCareerAnalysis;
import com.skilize.ai.domain.repository.AiCareerAnalysisRepository;
import com.skilize.ai.infrastructure.persistence.entity.AiCareerAnalysisEntity;
import com.skilize.ai.infrastructure.persistence.mapper.AiCareerAnalysisPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** domain.repository.AiCareerAnalysisRepository の実装。 */
@Repository
@RequiredArgsConstructor
public class AiCareerAnalysisRepositoryImpl implements AiCareerAnalysisRepository {

    private final AiCareerAnalysisJpaRepository jpaRepository;
    private final AiCareerAnalysisPersistenceMapper mapper;

    @Override
    public List<AiCareerAnalysis> findByUserIdOrderByFiscalYearIdDesc(int userId) {
        return jpaRepository.findByUserIdOrderByFiscalYearIdDesc(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<AiCareerAnalysis> findByUserIdAndFiscalYearId(int userId, int fiscalYearId) {
        return jpaRepository.findByUserIdAndFiscalYearId(userId, fiscalYearId).map(mapper::toDomain);
    }

    @Override
    public AiCareerAnalysis save(AiCareerAnalysis analysis) {
        AiCareerAnalysisEntity entity;
        if (analysis.getId() == null) {
            entity = AiCareerAnalysisEntity.createPending(analysis.getUserId(), analysis.getFiscalYearId());
        } else {
            entity = jpaRepository.findById(analysis.getId())
                    .orElseThrow(() -> new IllegalStateException("AiCareerAnalysis not found: id=" + analysis.getId()));
            // resetToPending() のみが既存レコードを変更するドメインメソッドのため、その状態のみを反映する
            if (analysis.getStatus() == AiAnalysisStatus.PENDING) {
                entity.resetToPending();
            }
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }
}
