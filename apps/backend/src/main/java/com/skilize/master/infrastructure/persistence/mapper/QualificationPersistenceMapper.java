/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * QualificationEntity（永続化モデル）と Qualification（ドメインモデル）を相互変換するMapper。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.mapper;

import com.skilize.master.domain.model.Qualification;
import com.skilize.master.infrastructure.persistence.entity.QualificationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** QualificationEntity ⇄ Qualification の変換を担うMapper。category は QualificationCategoryPersistenceMapper に委譲する。 */
@Component
@RequiredArgsConstructor
public class QualificationPersistenceMapper {

    private final QualificationCategoryPersistenceMapper categoryMapper;

    /** JPAエンティティからドメインモデルへ変換する。 */
    public Qualification toDomain(QualificationEntity entity) {
        if (entity == null) return null;
        return Qualification.reconstruct(entity.getId(), categoryMapper.toDomain(entity.getCategory()), entity.getName(),
                entity.getDescription(), entity.getSortOrder(), entity.isActive(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
