/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.QualificationCategoryRepository の実装クラス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.repository;

import com.skilize.master.domain.model.QualificationCategory;
import com.skilize.master.domain.repository.QualificationCategoryRepository;
import com.skilize.master.infrastructure.persistence.entity.QualificationCategoryEntity;
import com.skilize.master.infrastructure.persistence.mapper.QualificationCategoryPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** domain.repository.QualificationCategoryRepository の実装。 */
@Repository
@RequiredArgsConstructor
public class QualificationCategoryRepositoryImpl implements QualificationCategoryRepository {

    private final QualificationCategoryJpaRepository jpaRepository;
    private final QualificationCategoryPersistenceMapper mapper;

    @Override
    public Optional<QualificationCategory> findById(Integer id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public QualificationCategory save(QualificationCategory category) {
        QualificationCategoryEntity entity;
        if (category.getId() == null) {
            entity = QualificationCategoryEntity.create(category.getName(), category.getSortOrder());
        } else {
            entity = jpaRepository.findById(category.getId())
                    .orElseThrow(() -> new IllegalStateException("QualificationCategory not found: id=" + category.getId()));
            entity.update(category.getName(), category.getSortOrder(), category.isActive());
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<QualificationCategory> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<QualificationCategory> findByActiveTrueOrderBySortOrderAsc() {
        return jpaRepository.findByActiveTrueOrderBySortOrderAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<QualificationCategory> findAllByOrderBySortOrderAsc() {
        return jpaRepository.findAllByOrderBySortOrderAsc().stream().map(mapper::toDomain).toList();
    }
}
