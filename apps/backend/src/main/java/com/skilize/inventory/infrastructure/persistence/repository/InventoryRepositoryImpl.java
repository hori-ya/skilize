/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.InventoryRepository の実装クラス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.repository;

import com.skilize.fiscalyear.infrastructure.persistence.repository.FiscalYearJpaRepository;
import com.skilize.inventory.domain.model.Inventory;
import com.skilize.inventory.domain.repository.InventoryRepository;
import com.skilize.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.skilize.inventory.infrastructure.persistence.mapper.InventoryPersistenceMapper;
import com.skilize.user.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** domain.repository.InventoryRepository の実装。 */
@Repository
@RequiredArgsConstructor
public class InventoryRepositoryImpl implements InventoryRepository {

    private final InventoryJpaRepository jpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final FiscalYearJpaRepository fiscalYearJpaRepository;
    private final InventoryPersistenceMapper mapper;

    @Override
    public Optional<Inventory> findById(Integer id) {
        Optional<InventoryEntity> entityOptional = jpaRepository.findById(id);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(entityOptional.get()));
    }

    @Override
    public Inventory save(Inventory inventory) {
        InventoryEntity entity;
        if (inventory.getId() == null) {
            entity = InventoryEntity.create(userJpaRepository.getReferenceById(inventory.getUser().getId()),
                    fiscalYearJpaRepository.getReferenceById(inventory.getFiscalYear().getId()));
        } else {
            Optional<InventoryEntity> entityOptional = jpaRepository.findById(inventory.getId());
            if (entityOptional.isEmpty()) {
                throw new IllegalStateException("Inventory not found: id=" + inventory.getId());
            }
            entity = entityOptional.get();
        }
        // ステータス・タイムスタンプはドメインメソッド（submit/completeGoalReview/completeGoal）側で計算済みの値をそのまま反映する
        entity.applyState(inventory.getStatus(), inventory.getSubmittedAt(),
                inventory.getGoalReviewCompletedAt(), inventory.getGoalCompletedAt());
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Inventory> findByIdWithAssociations(int id) {
        Optional<InventoryEntity> entityOptional = jpaRepository.findByIdWithAssociations(id);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(entityOptional.get()));
    }

    @Override
    public List<Inventory> findByUserIdWithFiscalYear(int userId) {
        List<Inventory> inventories = new ArrayList<>();
        for (InventoryEntity entity : jpaRepository.findByUserIdWithFiscalYear(userId)) {
            inventories.add(mapper.toDomain(entity));
        }
        return inventories;
    }

    @Override
    public Optional<Inventory> findByUserIdAndFiscalYearId(int userId, int fiscalYearId) {
        Optional<InventoryEntity> entityOptional = jpaRepository.findByUserIdAndFiscalYearId(userId, fiscalYearId);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(entityOptional.get()));
    }
}
