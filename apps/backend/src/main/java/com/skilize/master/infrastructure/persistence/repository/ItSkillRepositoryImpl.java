/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.ItSkillRepository の実装クラス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.repository;

import com.skilize.master.domain.model.ItSkill;
import com.skilize.master.domain.repository.ItSkillRepository;
import com.skilize.master.infrastructure.persistence.entity.ItSkillCategoryEntity;
import com.skilize.master.infrastructure.persistence.entity.ItSkillEntity;
import com.skilize.master.infrastructure.persistence.mapper.ItSkillPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** domain.repository.ItSkillRepository の実装。 */
@Repository
@RequiredArgsConstructor
public class ItSkillRepositoryImpl implements ItSkillRepository {

    private final ItSkillJpaRepository jpaRepository;
    private final ItSkillCategoryJpaRepository categoryJpaRepository;
    private final ItSkillPersistenceMapper mapper;

    @Override
    public Optional<ItSkill> findById(Integer id) {
        Optional<ItSkillEntity> entityOptional = jpaRepository.findById(id);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(entityOptional.get()));
    }

    @Override
    public ItSkill save(ItSkill itSkill) {
        Optional<ItSkillCategoryEntity> categoryOptional = categoryJpaRepository.findById(itSkill.getCategory().getId());
        if (categoryOptional.isEmpty()) {
            throw new IllegalStateException("ItSkillCategory not found: id=" + itSkill.getCategory().getId());
        }
        ItSkillCategoryEntity categoryEntity = categoryOptional.get();
        ItSkillEntity entity;
        if (itSkill.getId() == null) {
            entity = ItSkillEntity.create(categoryEntity, itSkill.getName(), itSkill.getDescription(), itSkill.getSortOrder());
        } else {
            Optional<ItSkillEntity> entityOptional = jpaRepository.findById(itSkill.getId());
            if (entityOptional.isEmpty()) {
                throw new IllegalStateException("ItSkill not found: id=" + itSkill.getId());
            }
            entity = entityOptional.get();
            entity.update(categoryEntity, itSkill.getName(), itSkill.getDescription(), itSkill.getSortOrder(), itSkill.isActive());
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<ItSkill> findAll() {
        List<ItSkill> skills = new ArrayList<>();
        for (ItSkillEntity entity : jpaRepository.findAll()) {
            skills.add(mapper.toDomain(entity));
        }
        return skills;
    }

    @Override
    public List<ItSkill> findAllOrderByHierarchy() {
        List<ItSkill> skills = new ArrayList<>();
        for (ItSkillEntity entity : jpaRepository.findAllOrderByHierarchy()) {
            skills.add(mapper.toDomain(entity));
        }
        return skills;
    }

    @Override
    public List<ItSkill> findAllActiveWithCategory() {
        List<ItSkill> skills = new ArrayList<>();
        for (ItSkillEntity entity : jpaRepository.findAllActiveWithCategory()) {
            skills.add(mapper.toDomain(entity));
        }
        return skills;
    }

    @Override
    public List<ItSkill> findByActiveFalseOrderByHierarchy() {
        List<ItSkill> skills = new ArrayList<>();
        for (ItSkillEntity entity : jpaRepository.findByActiveFalseOrderByHierarchy()) {
            skills.add(mapper.toDomain(entity));
        }
        return skills;
    }
}
