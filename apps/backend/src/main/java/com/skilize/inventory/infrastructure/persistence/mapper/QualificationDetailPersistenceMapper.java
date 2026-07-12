/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * QualificationDetailEntity（永続化モデル）と QualificationDetail（ドメインモデル）を相互変換するMapper。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.mapper;

import com.skilize.inventory.domain.model.QualificationDetail;
import com.skilize.inventory.infrastructure.persistence.entity.QualificationDetailEntity;
import com.skilize.master.infrastructure.persistence.mapper.QualificationPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** QualificationDetailEntity ⇄ QualificationDetail の変換を担うMapper。qualification はmaster側のMapperに委譲する。 */
@Component
@RequiredArgsConstructor
public class QualificationDetailPersistenceMapper {

    private final QualificationPersistenceMapper qualificationMapper;

    /** JPAエンティティからドメインモデルへ変換する。 */
    public QualificationDetail toDomain(QualificationDetailEntity entity) {
        if (entity == null) return null;
        return QualificationDetail.reconstruct(entity.getId(), entity.getInventory().getId(),
                qualificationMapper.toDomain(entity.getQualification()), entity.getCustomQualificationName(),
                entity.getAcquiredYearMonth(), entity.getRemarks(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
