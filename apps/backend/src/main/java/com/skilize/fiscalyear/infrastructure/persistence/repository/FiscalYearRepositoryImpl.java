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
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<FiscalYear> findById(Integer id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public FiscalYear save(FiscalYear fiscalYear) {
        FiscalYearEntity entity;
        if (fiscalYear.getId() == null) {
            entity = FiscalYearEntity.create(fiscalYear.getName(), fiscalYear.getStartDate(), fiscalYear.getEndDate(),
                    fiscalYear.getInputStartDate(), fiscalYear.getInputEndDate());
        } else {
            entity = jpaRepository.findById(fiscalYear.getId())
                    .orElseThrow(() -> new IllegalStateException("FiscalYear not found: id=" + fiscalYear.getId()));
            entity.update(fiscalYear.getName(), fiscalYear.getStartDate(), fiscalYear.getEndDate(),
                    fiscalYear.getInputStartDate(), fiscalYear.getInputEndDate(), fiscalYear.isActive());
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<FiscalYear> findCurrent(LocalDate today) {
        return jpaRepository.findCurrent(today).map(mapper::toDomain);
    }
}
