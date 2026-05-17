package com.skilize.ai.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiCareerAnalysisRepository extends JpaRepository<AiCareerAnalysis, Integer> {

    List<AiCareerAnalysis> findByUserIdOrderByFiscalYearIdDesc(int userId);

    Optional<AiCareerAnalysis> findByUserIdAndFiscalYearId(int userId, int fiscalYearId);
}
