package com.skilize.master.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * ITスキルリポジトリ。カテゴリを JOIN FETCH で取得し N+1 を回避する。
 * ソート順は「カテゴリの表示順 → スキルの表示順」の2段階。
 */
public interface ItSkillRepository extends JpaRepository<ItSkill, Integer> {

    /** 全ITスキルをカテゴリ付きで取得する（有効・無効含む全件）。 */
    @Query("SELECT s FROM ItSkill s JOIN FETCH s.category ORDER BY s.category.sortOrder ASC, s.sortOrder ASC")
    List<ItSkill> findAllWithCategory();

    /** 有効なITスキルのみをカテゴリ付きで取得する。棚卸入力画面の選択肢に使用。 */
    @Query("SELECT s FROM ItSkill s JOIN FETCH s.category WHERE s.active = true ORDER BY s.category.sortOrder ASC, s.sortOrder ASC")
    List<ItSkill> findAllActiveWithCategory();

    /** 有効フラグを指定してITスキルをカテゴリ付きで取得する。false を渡すと無効スキル一覧になる。 */
    @Query("SELECT s FROM ItSkill s JOIN FETCH s.category WHERE s.active = :active ORDER BY s.category.sortOrder ASC, s.sortOrder ASC")
    List<ItSkill> findAllWithCategoryByActive(@Param("active") boolean active);
}
