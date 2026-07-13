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

import java.util.ArrayList;
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
        Optional<QualificationCategoryEntity> entityOptional = jpaRepository.findById(id);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(entityOptional.get()));
    }

    @Override
    public QualificationCategory save(QualificationCategory category) {
        QualificationCategoryEntity entity;
        if (category.getId() == null) {
            entity = QualificationCategoryEntity.create(category.getName(), category.getSortOrder());
        } else {
            Optional<QualificationCategoryEntity> entityOptional = jpaRepository.findById(category.getId());
            if (entityOptional.isEmpty()) {
                throw new IllegalStateException("QualificationCategory not found: id=" + category.getId());
            }
            entity = entityOptional.get();
            entity.update(category.getName(), category.getSortOrder(), category.isActive());
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<QualificationCategory> findAll() {
        List<QualificationCategory> categories = new ArrayList<>();
        for (QualificationCategoryEntity entity : jpaRepository.findAll()) {
            categories.add(mapper.toDomain(entity));
        }
        return categories;
    }

    @Override
    public List<QualificationCategory> findByActiveTrueOrderBySortOrderAsc() {
        List<QualificationCategory> categories = new ArrayList<>();
        for (QualificationCategoryEntity entity : jpaRepository.findByActiveTrueOrderBySortOrderAsc()) {
            categories.add(mapper.toDomain(entity));
        }
        return categories;
    }

    @Override
    public List<QualificationCategory> findAllByOrderBySortOrderAsc() {
        List<QualificationCategory> categories = new ArrayList<>();
        for (QualificationCategoryEntity entity : jpaRepository.findAllByOrderBySortOrderAsc()) {
            categories.add(mapper.toDomain(entity));
        }
        return categories;
    }
}
