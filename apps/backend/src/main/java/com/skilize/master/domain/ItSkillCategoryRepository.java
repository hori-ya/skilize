package com.skilize.master.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** ITスキル分類リポジトリ。最大3階層の自己参照構造を持つ。 */
public interface ItSkillCategoryRepository extends JpaRepository<ItSkillCategory, Integer> {
    /** 有効な分類を表示順昇順で返す。棚卸入力画面の選択肢に使用。 */
    List<ItSkillCategory> findByActiveTrueOrderBySortOrderAsc();
    /** 指定レベル（1=大分類, 2=中分類, 3=小分類）の有効な分類を返す。 */
    List<ItSkillCategory> findByLevelAndActiveTrueOrderBySortOrderAsc(short level);
    /** 全分類を階層レベル昇順・表示順昇順で返す。マスタ管理画面の一覧表示に使用。 */
    List<ItSkillCategory> findAllByOrderByLevelAscSortOrderAsc();
}
