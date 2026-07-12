/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ItSkillCategoryEntity（永続化モデル）と ItSkillCategory（ドメインモデル）を相互変換するMapper。
 * 親カテゴリ（parent）は最大3階層のため再帰的に変換する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.mapper;

import com.skilize.master.domain.model.ItSkillCategory;
import com.skilize.master.infrastructure.persistence.entity.ItSkillCategoryEntity;
import org.springframework.stereotype.Component;

/** ItSkillCategoryEntity ⇄ ItSkillCategory の変換を担うMapper。最大3階層の parent 関連を再帰的に変換する。 */
@Component
public class ItSkillCategoryPersistenceMapper {

    /** JPAエンティティからドメインモデルへ変換する（parent を再帰的に変換）。 */
    public ItSkillCategory toDomain(ItSkillCategoryEntity entity) {
        if (entity == null) return null;
        return ItSkillCategory.reconstruct(entity.getId(), entity.getParentId(), toDomain(entity.getParent()),
                entity.getLevel(), entity.getName(), entity.getSortOrder(), entity.isActive(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
