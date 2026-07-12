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
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public InventoryGoal save(InventoryGoal goal) {
        return mapper.toDomain(jpaRepository.save(toEntity(goal)));
    }

    @Override
    public List<InventoryGoal> saveAll(List<InventoryGoal> goals) {
        List<InventoryGoalEntity> entities = goals.stream().map(this::toEntity).toList();
        return jpaRepository.saveAll(entities).stream().map(mapper::toDomain).toList();
    }

    private InventoryGoalEntity toEntity(InventoryGoal goal) {
        if (goal.getId() == null) {
            ItSkillEntity itSkillEntity = goal.getItSkill() != null
                    ? itSkillJpaRepository.getReferenceById(goal.getItSkill().getId()) : null;
            QualificationEntity qualificationEntity = goal.getQualification() != null
                    ? qualificationJpaRepository.getReferenceById(goal.getQualification().getId()) : null;
            AdSeminarEntity adSeminarEntity = goal.getAdSeminar() != null
                    ? adSeminarJpaRepository.getReferenceById(goal.getAdSeminar().getId()) : null;
            return InventoryGoalEntity.create(inventoryJpaRepository.getReferenceById(goal.getInventoryId()),
                    goal.getGoalCategory(), itSkillEntity, qualificationEntity, adSeminarEntity,
                    goal.getCustomName(), goal.getTargetPeriod(), goal.getReason());
        }
        InventoryGoalEntity entity = jpaRepository.findById(goal.getId())
                .orElseThrow(() -> new IllegalStateException("InventoryGoal not found: id=" + goal.getId()));
        entity.updateReview(goal.getAchievementStatus(), goal.getReviewNote());
        return entity;
    }

    @Override
    public List<InventoryGoal> findByInventoryId(int inventoryId) {
        return jpaRepository.findByInventoryId(inventoryId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<InventoryGoal> findByInventoryIdForReport(int inventoryId) {
        return jpaRepository.findByInventoryIdForReport(inventoryId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteByInventoryId(int inventoryId) {
        jpaRepository.deleteByInventoryId(inventoryId);
    }
}
