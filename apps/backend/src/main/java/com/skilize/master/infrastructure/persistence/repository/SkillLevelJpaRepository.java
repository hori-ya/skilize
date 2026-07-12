/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * スキルレベルマスタの Spring Data JPA リポジトリ。レベル値昇順・有効フィルタ取得を提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.SkillLevelRepository から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.repository;

import com.skilize.master.infrastructure.persistence.entity.SkillLevelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** スキルレベル Spring Data JPA リポジトリ。他featureが直接JPA関連で参照する場合はこちらを直接injectしてよい。 */
public interface SkillLevelJpaRepository extends JpaRepository<SkillLevelEntity, Integer> {
    /** 全スキルレベルをレベル値昇順で返す。 */
    List<SkillLevelEntity> findAllByOrderByLevelValueAsc();
    /** 有効フラグを指定してスキルレベルをレベル値昇順で返す。 */
    List<SkillLevelEntity> findByActiveOrderByLevelValueAsc(boolean active);
}
