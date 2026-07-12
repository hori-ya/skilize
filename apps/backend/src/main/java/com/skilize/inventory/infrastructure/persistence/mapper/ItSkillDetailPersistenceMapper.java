/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ItSkillDetailEntity（永続化モデル）と ItSkillDetail（ドメインモデル）を相互変換するMapper。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.mapper;

import com.skilize.inventory.domain.model.ItSkillDetail;
import com.skilize.inventory.infrastructure.persistence.entity.ItSkillDetailEntity;
import com.skilize.master.infrastructure.persistence.mapper.ItSkillPersistenceMapper;
import com.skilize.master.infrastructure.persistence.mapper.SkillLevelPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** ItSkillDetailEntity ⇄ ItSkillDetail の変換を担うMapper。itSkill/skillLevel はmaster側のMapperに委譲する。 */
@Component
@RequiredArgsConstructor
public class ItSkillDetailPersistenceMapper {

    private final ItSkillPersistenceMapper itSkillMapper;
    private final SkillLevelPersistenceMapper skillLevelMapper;

    /** JPAエンティティからドメインモデルへ変換する。 */
    public ItSkillDetail toDomain(ItSkillDetailEntity entity) {
        if (entity == null) return null;
        return ItSkillDetail.reconstruct(entity.getId(), entity.getInventory().getId(),
                itSkillMapper.toDomain(entity.getItSkill()), entity.getCustomSkillName(),
                skillLevelMapper.toDomain(entity.getSkillLevel()), entity.getRemarks(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
