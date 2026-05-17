package com.skilize.expectation.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 期待情報リポジトリ。ユーザーごとに1件のみ存在する（ユーザー内部ID が PK）。 */
public interface UserExpectationRepository extends JpaRepository<UserExpectation, Integer> {
    /** ユーザー内部ID で期待情報を取得する。レコードが存在しない場合は Optional.empty() を返す。 */
    Optional<UserExpectation> findByUserId(Integer userId);
}
