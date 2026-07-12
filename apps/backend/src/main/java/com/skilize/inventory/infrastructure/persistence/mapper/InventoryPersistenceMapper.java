/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * InventoryEntity（永続化モデル）と Inventory（ドメインモデル）を相互変換するMapper。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.mapper;

import com.skilize.fiscalyear.infrastructure.persistence.mapper.FiscalYearPersistenceMapper;
import com.skilize.inventory.domain.model.Inventory;
import com.skilize.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.skilize.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** InventoryEntity ⇄ Inventory の変換を担うMapper。user は UserPersistenceMapper、fiscalYear は FiscalYearPersistenceMapper に委譲する。 */
@Component
@RequiredArgsConstructor
public class InventoryPersistenceMapper {

    private final UserPersistenceMapper userMapper;
    private final FiscalYearPersistenceMapper fiscalYearMapper;

    /** JPAエンティティからドメインモデルへ変換する。 */
    public Inventory toDomain(InventoryEntity entity) {
        if (entity == null) return null;
        return Inventory.reconstruct(entity.getId(), userMapper.toDomain(entity.getUser()),
                fiscalYearMapper.toDomain(entity.getFiscalYear()), entity.getStatus(),
                entity.getSubmittedAt(), entity.getGoalReviewCompletedAt(), entity.getGoalCompletedAt(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
