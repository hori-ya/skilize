package com.skilize.fiscalyear.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 年度設定リポジトリ。シングルトン（id=1 のレコード1件のみ）のため、カスタムクエリは不要。
 * findById((short) 1) で取得する。
 */
public interface FiscalYearSettingsRepository extends JpaRepository<FiscalYearSettings, Short> {}
