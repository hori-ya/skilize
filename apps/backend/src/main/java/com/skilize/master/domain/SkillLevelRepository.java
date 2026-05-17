package com.skilize.master.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** スキルレベルリポジトリ。 */
public interface SkillLevelRepository extends JpaRepository<SkillLevel, Integer> {
    /** 全スキルレベルをレベル値昇順で返す。 */
    List<SkillLevel> findAllByOrderByLevelValueAsc();
    /** 有効フラグを指定してスキルレベルをレベル値昇順で返す。 */
    List<SkillLevel> findByActiveOrderByLevelValueAsc(boolean active);
}
