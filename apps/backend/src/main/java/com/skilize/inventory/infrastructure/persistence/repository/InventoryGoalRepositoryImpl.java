/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.InventoryGoalRepository の実装クラス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.repository;

import com.skilize.inventory.domain.model.InventoryGoal;
import com.skilize.inventory.domain.repository.InventoryGoalRepository;
import com.skilize.inventory.infrastructure.persistence.entity.InventoryGoalEntity;
import com.skilize.inventory.infrastructure.persistence.mapper.InventoryGoalPersistenceMapper;
import com.skilize.master.infrastructure.persistence.entity.AdSeminarEntity;
import com.skilize.master.infrastructure.persistence.entity.ItSkillEntity;
import com.skilize.master.infrastructure.persistence.entity.QualificationEntity;
import com.skilize.master.infrastructure.persistence.repository.AdSeminarJpaRepository;
import com.skilize.master.infrastructure.persistence.repository.ItSkillJpaRepository;
import com.skilize.master.infrastructure.persistence.repository.QualificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** domain.repository.InventoryGoalRepository の実装。 */
@Repository
@RequiredArgsConstructor
public class InventoryGoalRepositoryImpl implements InventoryGoalRepository {

    private final InventoryGoalJpaRepository jpaRepository;
    private final InventoryJpaRepository inventoryJpaRepository;
    private final ItSkillJpaRepository itSkillJpaRepository;
    private final QualificationJpaRepository qualificationJpaRepository;
    private final AdSeminarJpaRepository adSeminarJpaRepository;
    private final InventoryGoalPersistenceMapper mapper;

    @Override
    public Optional<InventoryGoal> findById(Integer id) {
        Optional<InventoryGoalEntity> entityOptional = jpaRepository.findById(id);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(entityOptional.get()));
    }

    @Override
    public InventoryGoal save(InventoryGoal goal) {
        return mapper.toDomain(jpaRepository.save(toEntity(goal)));
    }

    @Override
    public List<InventoryGoal> saveAll(List<InventoryGoal> goals) {
        List<InventoryGoalEntity> entities = new ArrayList<>();
        for (InventoryGoal goal : goals) {
            entities.add(toEntity(goal));
        }
        List<InventoryGoal> saved = new ArrayList<>();
        for (InventoryGoalEntity entity : jpaRepository.saveAll(entities)) {
            saved.add(mapper.toDomain(entity));
        }
        return saved;
    }

    private InventoryGoalEntity toEntity(InventoryGoal goal) {
        if (goal.getId() == null) {
            ItSkillEntity itSkillEntity = null;
            if (goal.getItSkill() != null) {
                itSkillEntity = itSkillJpaRepository.getReferenceById(goal.getItSkill().getId());
            }
            QualificationEntity qualificationEntity = null;
            if (goal.getQualification() != null) {
                qualificationEntity = qualificationJpaRepository.getReferenceById(goal.getQualification().getId());
            }
            AdSeminarEntity adSeminarEntity = null;
            if (goal.getAdSeminar() != null) {
                adSeminarEntity = adSeminarJpaRepository.getReferenceById(goal.getAdSeminar().getId());
            }
            return InventoryGoalEntity.create(inventoryJpaRepository.getReferenceById(goal.getInventoryId()),
                    goal.getGoalCategory(), itSkillEntity, qualificationEntity, adSeminarEntity,
                    goal.getCustomName(), goal.getTargetPeriod(), goal.getReason());
        }
        Optional<InventoryGoalEntity> entityOptional = jpaRepository.findById(goal.getId());
        if (entityOptional.isEmpty()) {
            throw new IllegalStateException("InventoryGoal not found: id=" + goal.getId());
        }
        InventoryGoalEntity entity = entityOptional.get();
        entity.updateReview(goal.getAchievementStatus(), goal.getReviewNote());
        return entity;
    }

    @Override
    public List<InventoryGoal> findByInventoryId(int inventoryId) {
        List<InventoryGoal> goals = new ArrayList<>();
        for (InventoryGoalEntity entity : jpaRepository.findByInventoryId(inventoryId)) {
            goals.add(mapper.toDomain(entity));
        }
        return goals;
    }

    @Override
    public List<InventoryGoal> findByInventoryIdForReport(int inventoryId) {
        List<InventoryGoal> goals = new ArrayList<>();
        for (InventoryGoalEntity entity : jpaRepository.findByInventoryIdForReport(inventoryId)) {
            goals.add(mapper.toDomain(entity));
        }
        return goals;
    }

    @Override
    public void deleteByInventoryId(int inventoryId) {
        jpaRepository.deleteByInventoryId(inventoryId);
    }
}
