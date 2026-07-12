/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ADセミナーリポジトリインターフェース。永続化の実装詳細を持たない。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のAdSeminarRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.domain.repository;

import com.skilize.master.domain.model.AdSeminar;

import java.util.List;
import java.util.Optional;

/**
 * ADセミナーリポジトリ。実装は infrastructure.persistence.repository.AdSeminarRepositoryImpl。
 * ADセミナーは分類なし（category=null）の場合があるため、実装側は LEFT JOIN で分類を取得する。
 */
public interface AdSeminarRepository {

    /** IDでADセミナーを取得する。 */
    Optional<AdSeminar> findById(Integer id);

    /** ADセミナーを保存する（新規作成・更新の両方に使用）。 */
    AdSeminar save(AdSeminar adSeminar);

    /** 全ADセミナーを返す（順不同）。 */
    List<AdSeminar> findAll();

    /** 全ADセミナーをマスタ管理画面向けソート順（分類→並順）で取得する（有効・無効含む全件）。 */
    List<AdSeminar> findAllWithCategory();

    /** 有効なADセミナーのみを分類付きで取得する。棚卸入力画面の選択肢に使用（ソート順は変更なし）。 */
    List<AdSeminar> findAllActiveWithCategory();

    /** 有効フラグを指定してADセミナーをマスタ管理画面向けソート順（分類→並順）で取得する。 */
    List<AdSeminar> findAllWithCategoryByActive(boolean active);
}
