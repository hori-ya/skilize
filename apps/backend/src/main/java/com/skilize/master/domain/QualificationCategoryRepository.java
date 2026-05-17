package com.skilize.master.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 資格分類リポジトリ。フラット構造（階層なし）。 */
public interface QualificationCategoryRepository extends JpaRepository<QualificationCategory, Integer> {
    /** 有効な資格分類を表示順昇順で返す。棚卸入力画面の選択肢に使用。 */
    List<QualificationCategory> findByActiveTrueOrderBySortOrderAsc();
    /** 全資格分類（有効・無効含む）を表示順昇順で返す。マスタ管理画面に使用。 */
    List<QualificationCategory> findAllByOrderBySortOrderAsc();
}
