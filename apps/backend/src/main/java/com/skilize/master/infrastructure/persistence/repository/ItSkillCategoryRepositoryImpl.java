/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.ItSkillCategoryRepository の実装クラス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.repository;

import com.skilize.master.domain.model.ItSkillCategory;
import com.skilize.master.domain.repository.ItSkillCategoryRepository;
import com.skilize.master.infrastructure.persistence.entity.ItSkillCategoryEntity;
import com.skilize.master.infrastructure.persistence.mapper.ItSkillCategoryPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** domain.repository.ItSkillCategoryRepository の実装。 */
@Repository
@RequiredArgsConstructor
public class ItSkillCategoryRepositoryImpl implements ItSkillCategoryRepository {

    private final ItSkillCategoryJpaRepository jpaRepository;
    private final ItSkillCategoryPersistenceMapper mapper;

    @Override
    public Optional<ItSkillCategory> findById(Integer id) {
        Optional<ItSkillCategoryEntity> entityOptional = jpaRepository.findById(id);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(entityOptional.get()));
    }

    @Override
    public ItSkillCategory save(ItSkillCategory category) {
        ItSkillCategoryEntity entity;
        if (category.getId() == null) {
            entity = ItSkillCategoryEntity.create(category.getParentId(), category.getLevel(), category.getName(), category.getSortOrder());
        } else {
            Optional<ItSkillCategoryEntity> entityOptional = jpaRepository.findById(category.getId());
            if (entityOptional.isEmpty()) {
                throw new IllegalStateException("ItSkillCategory not found: id=" + category.getId());
            }
            entity = entityOptional.get();
            entity.update(category.getName(), category.getSortOrder(), category.isActive());
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<ItSkillCategory> findAll() {
        List<ItSkillCategory> categories = new ArrayList<>();
        for (ItSkillCategoryEntity entity : jpaRepository.findAll()) {
            categories.add(mapper.toDomain(entity));
        }
        return categories;
    }

    @Override
    public List<ItSkillCategory> findByActiveTrueOrderBySortOrderAsc() {
        List<ItSkillCategory> categories = new ArrayList<>();
        for (ItSkillCategoryEntity entity : jpaRepository.findByActiveTrueOrderBySortOrderAsc()) {
            categories.add(mapper.toDomain(entity));
        }
        return categories;
    }

    @Override
    public List<ItSkillCategory> findByLevelAndActiveTrueOrderBySortOrderAsc(short level) {
        List<ItSkillCategory> categories = new ArrayList<>();
        for (ItSkillCategoryEntity entity : jpaRepository.findByLevelAndActiveTrueOrderBySortOrderAsc(level)) {
            categories.add(mapper.toDomain(entity));
        }
        return categories;
    }

    @Override
    public List<ItSkillCategory> findByActiveFalseOrderByLevelAscParentIdAscSortOrderAsc() {
        List<ItSkillCategory> categories = new ArrayList<>();
        for (ItSkillCategoryEntity entity : jpaRepository.findByActiveFalseOrderByLevelAscParentIdAscSortOrderAsc()) {
            categories.add(mapper.toDomain(entity));
        }
        return categories;
    }

    @Override
    public List<ItSkillCategory> findAllByOrderByLevelAscSortOrderAsc() {
        List<ItSkillCategory> categories = new ArrayList<>();
        for (ItSkillCategoryEntity entity : jpaRepository.findAllByOrderByLevelAscSortOrderAsc()) {
            categories.add(mapper.toDomain(entity));
        }
        return categories;
    }

    @Override
    public List<ItSkillCategory> findAllByOrderByLevelAscParentIdAscSortOrderAsc() {
        List<ItSkillCategory> categories = new ArrayList<>();
        for (ItSkillCategoryEntity entity : jpaRepository.findAllByOrderByLevelAscParentIdAscSortOrderAsc()) {
            categories.add(mapper.toDomain(entity));
        }
        return categories;
    }
}
