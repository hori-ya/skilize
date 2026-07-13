/**************************************************************************************************************
 * 機能ID      ：FY
 * 機能名      ：年度管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.FiscalYearRepository の実装クラス。
 * FiscalYearJpaRepository と FiscalYearPersistenceMapper を用いてドメインモデルとの変換を行う。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.fiscalyear.infrastructure.persistence.repository;

import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.fiscalyear.domain.repository.FiscalYearRepository;
import com.skilize.fiscalyear.infrastructure.persistence.entity.FiscalYearEntity;
import com.skilize.fiscalyear.infrastructure.persistence.mapper.FiscalYearPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** domain.repository.FiscalYearRepository の実装。JPAエンティティとドメインモデルの変換を担う。 */
@Repository
@RequiredArgsConstructor
public class FiscalYearRepositoryImpl implements FiscalYearRepository {

    private final FiscalYearJpaRepository jpaRepository;
    private final FiscalYearPersistenceMapper mapper;

    @Override
    public List<FiscalYear> findAll() {
        List<FiscalYear> fiscalYears = new ArrayList<>();
        for (FiscalYearEntity entity : jpaRepository.findAll()) {
            fiscalYears.add(mapper.toDomain(entity));
        }
        return fiscalYears;
    }

    @Override
    public Optional<FiscalYear> findById(Integer id) {
        Optional<FiscalYearEntity> entityOptional = jpaRepository.findById(id);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(entityOptional.get()));
    }

    @Override
    public FiscalYear save(FiscalYear fiscalYear) {
        FiscalYearEntity entity;
        if (fiscalYear.getId() == null) {
            entity = FiscalYearEntity.create(fiscalYear.getName(), fiscalYear.getStartDate(), fiscalYear.getEndDate(),
                    fiscalYear.getInputStartDate(), fiscalYear.getInputEndDate());
        } else {
            Optional<FiscalYearEntity> entityOptional = jpaRepository.findById(fiscalYear.getId());
            if (entityOptional.isEmpty()) {
                throw new IllegalStateException("FiscalYear not found: id=" + fiscalYear.getId());
            }
            entity = entityOptional.get();
            entity.update(fiscalYear.getName(), fiscalYear.getStartDate(), fiscalYear.getEndDate(),
                    fiscalYear.getInputStartDate(), fiscalYear.getInputEndDate(), fiscalYear.isActive());
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<FiscalYear> findCurrent(LocalDate today) {
        Optional<FiscalYearEntity> entityOptional = jpaRepository.findCurrent(today);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(entityOptional.get()));
    }
}
