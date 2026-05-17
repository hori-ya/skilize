package com.skilize.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * ユーザーリポジトリ。Spring Data JPA がメソッド名からクエリを自動生成する。
 */
public interface UserRepository extends JpaRepository<User, Integer> {
    /** ユーザーID文字列（例: "user01"）で検索する。認証時のユーザー特定に使用。 */
    Optional<User> findByUserId(String userId);
    /** 全ユーザーをユーザーID昇順で返す。管理画面の一覧表示用。 */
    List<User> findAllByOrderByUserIdAsc();
    /** 指定TLの担当メンバー（有効ユーザーのみ）を返す。TL向けチーム照会に使用。 */
    List<User> findByTlUserIdAndActiveTrue(int tlUserId);
    /** 有効ユーザー全員を返す。ADMIN 向けチーム照会に使用。 */
    List<User> findByActiveTrue();
}
