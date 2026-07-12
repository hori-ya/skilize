/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * SeminarDetailEntity（永続化モデル）と SeminarDetail（ドメインモデル）を相互変換するMapper。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.mapper;

import com.skilize.inventory.domain.model.SeminarDetail;
import com.skilize.inventory.infrastructure.persistence.entity.SeminarDetailEntity;
import com.skilize.master.infrastructure.persistence.mapper.AdSeminarPersistenceMapper;
import com.skilize.master.infrastructure.persistence.mapper.SeminarCategoryPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** SeminarDetailEntity ⇄ SeminarDetail の変換を担うMapper。adSeminar/seminarCategory はmaster側のMapperに委譲する。 */
@Component
@RequiredArgsConstructor
public class SeminarDetailPersistenceMapper {

    private final AdSeminarPersistenceMapper adSeminarMapper;
    private final SeminarCategoryPersistenceMapper seminarCategoryMapper;

    /** JPAエンティティからドメインモデルへ変換する。 */
    public SeminarDetail toDomain(SeminarDetailEntity entity) {
        if (entity == null) return null;
        return SeminarDetail.reconstruct(entity.getId(), entity.getInventory().getId(),
                adSeminarMapper.toDomain(entity.getAdSeminar()), entity.getSeminarName(),
                seminarCategoryMapper.toDomain(entity.getSeminarCategory()), entity.getAttendedYearMonth(),
                entity.getRemarks(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
