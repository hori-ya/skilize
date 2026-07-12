/**************************************************************************************************************
 * 機能ID      ：USR
 * 機能名      ：ユーザー管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ユーザーリポジトリインターフェース。永続化の実装詳細を持たない。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のUserRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.user.domain.repository;

import com.skilize.user.domain.model.User;

import java.util.List;
import java.util.Optional;

/** ユーザーリポジトリ。実装は infrastructure.persistence.repository.UserRepositoryImpl。 */
public interface UserRepository {

    /** IDでユーザーを取得する。 */
    Optional<User> findById(Integer id);

    /** ユーザーを保存する（新規作成・更新の両方に使用）。 */
    User save(User user);

    /** ユーザーID文字列（例: "user01"）で検索する。認証時のユーザー特定に使用。 */
    Optional<User> findByUserId(String userId);

    /** 全ユーザーをユーザーID昇順で返す。管理画面の一覧表示用。 */
    List<User> findAllByOrderByUserIdAsc();

    /** 指定TLの担当メンバー（有効ユーザーのみ）を返す。TL向けチーム照会に使用。 */
    List<User> findByTlUserIdAndActiveTrue(int tlUserId);

    /** 有効ユーザー全員を返す。ADMIN 向けチーム照会に使用。 */
    List<User> findByActiveTrue();
}
