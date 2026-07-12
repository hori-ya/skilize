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
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public ItSkillCategory save(ItSkillCategory category) {
        ItSkillCategoryEntity entity;
        if (category.getId() == null) {
            entity = ItSkillCategoryEntity.create(category.getParentId(), category.getLevel(), category.getName(), category.getSortOrder());
        } else {
            entity = jpaRepository.findById(category.getId())
                    .orElseThrow(() -> new IllegalStateException("ItSkillCategory not found: id=" + category.getId()));
            entity.update(category.getName(), category.getSortOrder(), category.isActive());
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<ItSkillCategory> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ItSkillCategory> findByActiveTrueOrderBySortOrderAsc() {
        return jpaRepository.findByActiveTrueOrderBySortOrderAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ItSkillCategory> findByLevelAndActiveTrueOrderBySortOrderAsc(short level) {
        return jpaRepository.findByLevelAndActiveTrueOrderBySortOrderAsc(level).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ItSkillCategory> findByActiveFalseOrderByLevelAscParentIdAscSortOrderAsc() {
        return jpaRepository.findByActiveFalseOrderByLevelAscParentIdAscSortOrderAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ItSkillCategory> findAllByOrderByLevelAscSortOrderAsc() {
        return jpaRepository.findAllByOrderByLevelAscSortOrderAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ItSkillCategory> findAllByOrderByLevelAscParentIdAscSortOrderAsc() {
        return jpaRepository.findAllByOrderByLevelAscParentIdAscSortOrderAsc().stream().map(mapper::toDomain).toList();
    }
}
