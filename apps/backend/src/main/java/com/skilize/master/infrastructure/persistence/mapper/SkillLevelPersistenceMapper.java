/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * SkillLevelEntity（永続化モデル）と SkillLevel（ドメインモデル）を相互変換するMapper。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.mapper;

import com.skilize.master.domain.model.SkillLevel;
import com.skilize.master.infrastructure.persistence.entity.SkillLevelEntity;
import org.springframework.stereotype.Component;

/** SkillLevelEntity ⇄ SkillLevel の変換を担うMapper。 */
@Component
public class SkillLevelPersistenceMapper {

    /** JPAエンティティからドメインモデルへ変換する。 */
    public SkillLevel toDomain(SkillLevelEntity entity) {
        if (entity == null) return null;
        return SkillLevel.reconstruct(entity.getId(), entity.getLevelValue(), entity.getDescription(),
                entity.getScoreWeight(), entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
