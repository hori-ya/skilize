/**************************************************************************************************************
 * 機能ID      ：EXP
 * 機能名      ：期待コメント
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ユーザー期待情報の永続化を担う Spring Data JPA リポジトリ。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.UserExpectationRepository から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.expectation.infrastructure.persistence.repository;

import com.skilize.expectation.infrastructure.persistence.entity.UserExpectationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** ユーザー期待情報 Spring Data JPA リポジトリ。ユーザーごとに1件のみ存在する（ユーザー内部ID が PK）。 */
public interface UserExpectationJpaRepository extends JpaRepository<UserExpectationEntity, Integer> {
    /** ユーザー内部ID で期待情報を取得する。レコードが存在しない場合は Optional.empty() を返す。 */
    Optional<UserExpectationEntity> findByUserId(Integer userId);
}
