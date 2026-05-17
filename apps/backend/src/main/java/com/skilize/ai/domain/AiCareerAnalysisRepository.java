package com.skilize.ai.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** AI キャリア分析リポジトリ。ユーザー × 年度の組み合わせで1件のレコードを管理する。 */
public interface AiCareerAnalysisRepository extends JpaRepository<AiCareerAnalysis, Integer> {

    /** 指定ユーザーの全分析結果を年度降順（新しい順）で返す。 */
    List<AiCareerAnalysis> findByUserIdOrderByFiscalYearIdDesc(int userId);

    /** 指定ユーザー・指定年度の分析結果を返す。分析トリガー時の既存レコード確認に使用。 */
    Optional<AiCareerAnalysis> findByUserIdAndFiscalYearId(int userId, int fiscalYearId);
}
