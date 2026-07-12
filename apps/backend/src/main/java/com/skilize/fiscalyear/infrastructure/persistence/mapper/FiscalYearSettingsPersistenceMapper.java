/**************************************************************************************************************
 * 機能ID      ：FY
 * 機能名      ：年度管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * FiscalYearSettingsEntity（永続化モデル）と FiscalYearSettings（ドメインモデル）を相互変換するMapper。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.fiscalyear.infrastructure.persistence.mapper;

import com.skilize.fiscalyear.domain.model.FiscalYearSettings;
import com.skilize.fiscalyear.infrastructure.persistence.entity.FiscalYearSettingsEntity;
import org.springframework.stereotype.Component;

/** FiscalYearSettingsEntity ⇄ FiscalYearSettings の変換を担うMapper。 */
@Component
public class FiscalYearSettingsPersistenceMapper {

    /** JPAエンティティからドメインモデルへ変換する。 */
    public FiscalYearSettings toDomain(FiscalYearSettingsEntity entity) {
        if (entity == null) return null;
        return FiscalYearSettings.reconstruct(entity.getId(), entity.getFiscalYearStartMonth(), entity.getUpdatedAt());
    }
}
