/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * InventoryGoalEntity（永続化モデル）と InventoryGoal（ドメインモデル）を相互変換するMapper。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.mapper;

import com.skilize.inventory.domain.model.InventoryGoal;
import com.skilize.inventory.infrastructure.persistence.entity.InventoryGoalEntity;
import com.skilize.master.infrastructure.persistence.mapper.AdSeminarPersistenceMapper;
import com.skilize.master.infrastructure.persistence.mapper.ItSkillPersistenceMapper;
import com.skilize.master.infrastructure.persistence.mapper.QualificationPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** InventoryGoalEntity ⇄ InventoryGoal の変換を担うMapper。itSkill/qualification/adSeminar はmaster側のMapperに委譲する。 */
@Component
@RequiredArgsConstructor
public class InventoryGoalPersistenceMapper {

    private final ItSkillPersistenceMapper itSkillMapper;
    private final QualificationPersistenceMapper qualificationMapper;
    private final AdSeminarPersistenceMapper adSeminarMapper;

    /** JPAエンティティからドメインモデルへ変換する。 */
    public InventoryGoal toDomain(InventoryGoalEntity entity) {
        if (entity == null) return null;
        return InventoryGoal.reconstruct(entity.getId(), entity.getInventory().getId(), entity.getGoalCategory(),
                itSkillMapper.toDomain(entity.getItSkill()), qualificationMapper.toDomain(entity.getQualification()),
                adSeminarMapper.toDomain(entity.getAdSeminar()), entity.getCustomName(), entity.getTargetPeriod(),
                entity.getReason(), entity.getAchievementStatus(), entity.getReviewNote(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
