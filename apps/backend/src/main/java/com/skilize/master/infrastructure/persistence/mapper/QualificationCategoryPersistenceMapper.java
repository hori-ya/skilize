/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * QualificationCategoryEntity（永続化モデル）と QualificationCategory（ドメインモデル）を相互変換するMapper。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.mapper;

import com.skilize.master.domain.model.QualificationCategory;
import com.skilize.master.infrastructure.persistence.entity.QualificationCategoryEntity;
import org.springframework.stereotype.Component;

/** QualificationCategoryEntity ⇄ QualificationCategory の変換を担うMapper。 */
@Component
public class QualificationCategoryPersistenceMapper {

    /** JPAエンティティからドメインモデルへ変換する。 */
    public QualificationCategory toDomain(QualificationCategoryEntity entity) {
        if (entity == null) return null;
        return QualificationCategory.reconstruct(entity.getId(), entity.getName(), entity.getSortOrder(),
                entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
