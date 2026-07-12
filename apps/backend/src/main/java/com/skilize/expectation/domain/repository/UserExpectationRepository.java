/**************************************************************************************************************
 * 機能ID      ：EXP
 * 機能名      ：期待コメント
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ユーザー期待情報リポジトリインターフェース。永続化の実装詳細を持たない。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のUserExpectationRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.expectation.domain.repository;

import com.skilize.expectation.domain.model.UserExpectation;

import java.util.Optional;

/** 期待情報リポジトリ。ユーザーごとに1件のみ存在する（ユーザー内部ID が PK）。実装は infrastructure.persistence.repository.UserExpectationRepositoryImpl。 */
public interface UserExpectationRepository {

    /** ユーザー内部ID で期待情報を取得する。レコードが存在しない場合は Optional.empty() を返す。 */
    Optional<UserExpectation> findByUserId(Integer userId);

    /** 期待情報を保存する（新規作成・更新の両方に使用）。 */
    UserExpectation save(UserExpectation expectation);
}
