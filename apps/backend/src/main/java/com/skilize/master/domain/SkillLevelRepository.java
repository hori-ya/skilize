/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * スキルレベルマスタのデータアクセスインターフェース。レベル値昇順・有効フィルタ取得を提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** スキルレベルリポジトリ。 */
public interface SkillLevelRepository extends JpaRepository<SkillLevel, Integer> {
    /** 全スキルレベルをレベル値昇順で返す。 */
    List<SkillLevel> findAllByOrderByLevelValueAsc();
    /** 有効フラグを指定してスキルレベルをレベル値昇順で返す。 */
    List<SkillLevel> findByActiveOrderByLevelValueAsc(boolean active);
}
