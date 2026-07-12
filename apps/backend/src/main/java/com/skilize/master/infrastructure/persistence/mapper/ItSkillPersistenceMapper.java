/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ItSkillEntity（永続化モデル）と ItSkill（ドメインモデル）を相互変換するMapper。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.mapper;

import com.skilize.master.domain.model.ItSkill;
import com.skilize.master.infrastructure.persistence.entity.ItSkillEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** ItSkillEntity ⇄ ItSkill の変換を担うMapper。category は ItSkillCategoryPersistenceMapper に委譲する。 */
@Component
@RequiredArgsConstructor
public class ItSkillPersistenceMapper {

    private final ItSkillCategoryPersistenceMapper categoryMapper;

    /** JPAエンティティからドメインモデルへ変換する。 */
    public ItSkill toDomain(ItSkillEntity entity) {
        if (entity == null) return null;
        return ItSkill.reconstruct(entity.getId(), categoryMapper.toDomain(entity.getCategory()), entity.getName(),
                entity.getDescription(), entity.getSortOrder(), entity.isActive(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
