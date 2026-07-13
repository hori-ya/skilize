/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.SkillLevelRepository の実装クラス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.repository;

import com.skilize.master.domain.model.SkillLevel;
import com.skilize.master.domain.repository.SkillLevelRepository;
import com.skilize.master.infrastructure.persistence.entity.SkillLevelEntity;
import com.skilize.master.infrastructure.persistence.mapper.SkillLevelPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** domain.repository.SkillLevelRepository の実装。 */
@Repository
@RequiredArgsConstructor
public class SkillLevelRepositoryImpl implements SkillLevelRepository {

    private final SkillLevelJpaRepository jpaRepository;
    private final SkillLevelPersistenceMapper mapper;

    @Override
    public Optional<SkillLevel> findById(Integer id) {
        Optional<SkillLevelEntity> entityOptional = jpaRepository.findById(id);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(entityOptional.get()));
    }

    @Override
    public SkillLevel save(SkillLevel skillLevel) {
        SkillLevelEntity entity;
        if (skillLevel.getId() == null) {
            entity = SkillLevelEntity.create(skillLevel.getLevelValue(), skillLevel.getDescription(), skillLevel.getScoreWeight());
        } else {
            Optional<SkillLevelEntity> entityOptional = jpaRepository.findById(skillLevel.getId());
            if (entityOptional.isEmpty()) {
                throw new IllegalStateException("SkillLevel not found: id=" + skillLevel.getId());
            }
            entity = entityOptional.get();
            entity.update(skillLevel.getLevelValue(), skillLevel.getDescription(), skillLevel.isActive(), skillLevel.getScoreWeight());
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<SkillLevel> findAllByOrderByLevelValueAsc() {
        List<SkillLevel> levels = new ArrayList<>();
        for (SkillLevelEntity entity : jpaRepository.findAllByOrderByLevelValueAsc()) {
            levels.add(mapper.toDomain(entity));
        }
        return levels;
    }

    @Override
    public List<SkillLevel> findByActiveOrderByLevelValueAsc(boolean active) {
        List<SkillLevel> levels = new ArrayList<>();
        for (SkillLevelEntity entity : jpaRepository.findByActiveOrderByLevelValueAsc(active)) {
            levels.add(mapper.toDomain(entity));
        }
        return levels;
    }
}
