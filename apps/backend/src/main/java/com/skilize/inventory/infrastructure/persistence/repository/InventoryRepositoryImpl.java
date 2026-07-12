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
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Inventory save(Inventory inventory) {
        InventoryEntity entity;
        if (inventory.getId() == null) {
            entity = InventoryEntity.create(userJpaRepository.getReferenceById(inventory.getUser().getId()),
                    fiscalYearJpaRepository.getReferenceById(inventory.getFiscalYear().getId()));
        } else {
            entity = jpaRepository.findById(inventory.getId())
                    .orElseThrow(() -> new IllegalStateException("Inventory not found: id=" + inventory.getId()));
        }
        // ステータス・タイムスタンプはドメインメソッド（submit/completeGoalReview/completeGoal）側で計算済みの値をそのまま反映する
        entity.applyState(inventory.getStatus(), inventory.getSubmittedAt(),
                inventory.getGoalReviewCompletedAt(), inventory.getGoalCompletedAt());
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Inventory> findByIdWithAssociations(int id) {
        return jpaRepository.findByIdWithAssociations(id).map(mapper::toDomain);
    }

    @Override
    public List<Inventory> findByUserIdWithFiscalYear(int userId) {
        return jpaRepository.findByUserIdWithFiscalYear(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Inventory> findByUserIdAndFiscalYearId(int userId, int fiscalYearId) {
        return jpaRepository.findByUserIdAndFiscalYearId(userId, fiscalYearId).map(mapper::toDomain);
    }
}
