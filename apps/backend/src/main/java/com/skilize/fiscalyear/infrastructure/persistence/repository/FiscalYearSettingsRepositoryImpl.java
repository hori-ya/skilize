/**************************************************************************************************************
 * 機能ID      ：FY
 * 機能名      ：年度管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.FiscalYearSettingsRepository の実装クラス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.fiscalyear.infrastructure.persistence.repository;

import com.skilize.fiscalyear.domain.model.FiscalYearSettings;
import com.skilize.fiscalyear.domain.repository.FiscalYearSettingsRepository;
import com.skilize.fiscalyear.infrastructure.persistence.entity.FiscalYearSettingsEntity;
import com.skilize.fiscalyear.infrastructure.persistence.mapper.FiscalYearSettingsPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** domain.repository.FiscalYearSettingsRepository の実装。 */
@Repository
@RequiredArgsConstructor
public class FiscalYearSettingsRepositoryImpl implements FiscalYearSettingsRepository {

    private final FiscalYearSettingsJpaRepository jpaRepository;
    private final FiscalYearSettingsPersistenceMapper mapper;

    @Override
    public Optional<FiscalYearSettings> findById(Short id) {
        Optional<FiscalYearSettingsEntity> entityOptional = jpaRepository.findById(id);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(entityOptional.get()));
    }

    @Override
    public FiscalYearSettings save(FiscalYearSettings settings) {
        Optional<FiscalYearSettingsEntity> entityOptional = jpaRepository.findById(settings.getId());
        if (entityOptional.isEmpty()) {
            throw new IllegalStateException("FiscalYearSettings not found: id=" + settings.getId());
        }
        FiscalYearSettingsEntity entity = entityOptional.get();
        entity.setFiscalYearStartMonth(settings.getFiscalYearStartMonth());
        return mapper.toDomain(jpaRepository.save(entity));
    }
}
