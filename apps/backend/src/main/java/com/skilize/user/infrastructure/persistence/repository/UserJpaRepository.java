/**************************************************************************************************************
 * 機能ID      ：USR
 * 機能名      ：ユーザー管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ユーザーエンティティの永続化を担う Spring Data JPA リポジトリ。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.UserRepository から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.user.infrastructure.persistence.repository;

import com.skilize.user.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * ユーザー Spring Data JPA リポジトリ。他featureが直接JPA関連（@ManyToOne/@OneToOne等）で
 * ユーザーエンティティを参照する必要がある場合（未移行feature側）は、このインターフェースを直接injectしてよい。
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, Integer> {
    /** ユーザーID文字列（例: "user01"）で検索する。認証時のユーザー特定に使用。 */
    Optional<UserEntity> findByUserId(String userId);
    /** 全ユーザーをユーザーID昇順で返す。管理画面の一覧表示用。 */
    List<UserEntity> findAllByOrderByUserIdAsc();
    /** 指定TLの担当メンバー（有効ユーザーのみ）を返す。TL向けチーム照会に使用。 */
    List<UserEntity> findByTlUserIdAndActiveTrue(int tlUserId);
    /** 有効ユーザー全員を返す。ADMIN 向けチーム照会に使用。 */
    List<UserEntity> findByActiveTrue();
}
