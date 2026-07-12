/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * スキルレベルリポジトリインターフェース。永続化の実装詳細を持たない。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のSkillLevelRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.domain.repository;

import com.skilize.master.domain.model.SkillLevel;

import java.util.List;
import java.util.Optional;

/** スキルレベルリポジトリ。実装は infrastructure.persistence.repository.SkillLevelRepositoryImpl。 */
public interface SkillLevelRepository {

    /** IDでスキルレベルを取得する。 */
    Optional<SkillLevel> findById(Integer id);

    /** スキルレベルを保存する（新規作成・更新の両方に使用）。 */
    SkillLevel save(SkillLevel skillLevel);

    /** 全スキルレベルをレベル値昇順で返す。 */
    List<SkillLevel> findAllByOrderByLevelValueAsc();

    /** 有効フラグを指定してスキルレベルをレベル値昇順で返す。 */
    List<SkillLevel> findByActiveOrderByLevelValueAsc(boolean active);
}
