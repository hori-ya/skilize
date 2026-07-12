/**************************************************************************************************************
 * 機能ID      ：FY
 * 機能名      ：年度管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 年度設定エンティティの永続化を担う Spring Data JPA リポジトリ（シングルトン）。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.FiscalYearSettingsRepository から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.fiscalyear.infrastructure.persistence.repository;

import com.skilize.fiscalyear.infrastructure.persistence.entity.FiscalYearSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** 年度設定 Spring Data JPA リポジトリ。シングルトン（id=1 のレコード1件のみ）のため、カスタムクエリは不要。 */
public interface FiscalYearSettingsJpaRepository extends JpaRepository<FiscalYearSettingsEntity, Short> {}
