/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * AdSeminarEntity（永続化モデル）と AdSeminar（ドメインモデル）を相互変換するMapper。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.mapper;

import com.skilize.master.domain.model.AdSeminar;
import com.skilize.master.infrastructure.persistence.entity.AdSeminarEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** AdSeminarEntity ⇄ AdSeminar の変換を担うMapper。category は AdSeminarCategoryPersistenceMapper に委譲する。 */
@Component
@RequiredArgsConstructor
public class AdSeminarPersistenceMapper {

    private final AdSeminarCategoryPersistenceMapper categoryMapper;

    /** JPAエンティティからドメインモデルへ変換する。 */
    public AdSeminar toDomain(AdSeminarEntity entity) {
        if (entity == null) return null;
        return AdSeminar.reconstruct(entity.getId(), categoryMapper.toDomain(entity.getCategory()), entity.getName(),
                entity.getDescription(), entity.getSortOrder(), entity.isActive(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
