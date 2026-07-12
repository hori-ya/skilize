/**************************************************************************************************************
 * 機能ID      ：FY
 * 機能名      ：年度管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * FiscalYearEntity（永続化モデル）と FiscalYear（ドメインモデル）を相互変換するMapper。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.fiscalyear.infrastructure.persistence.mapper;

import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.fiscalyear.infrastructure.persistence.entity.FiscalYearEntity;
import org.springframework.stereotype.Component;

/** FiscalYearEntity ⇄ FiscalYear の変換を担うMapper。 */
@Component
public class FiscalYearPersistenceMapper {

    /** JPAエンティティからドメインモデルへ変換する。 */
    public FiscalYear toDomain(FiscalYearEntity entity) {
        if (entity == null) return null;
        return FiscalYear.reconstruct(entity.getId(), entity.getName(), entity.getStartDate(), entity.getEndDate(),
                entity.getInputStartDate(), entity.getInputEndDate(), entity.isActive(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
