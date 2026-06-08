/**************************************************************************************************************
 * 機能ID      ：FY
 * 機能名      ：年度管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 年度・年度設定の作成・更新ビジネスロジックを担うサービスクラス（ADMIN 専用操作）。
 * 年度の有効期間と棚卸入力受付期間を独立して管理する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.fiscalyear.application;

import com.skilize.fiscalyear.domain.FiscalYear;
import com.skilize.fiscalyear.domain.FiscalYearRepository;
import com.skilize.fiscalyear.domain.FiscalYearSettings;
import com.skilize.fiscalyear.domain.FiscalYearSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/**
 * 年度・年度設定の作成・更新ビジネスロジック。ADMIN 専用操作。
 * 年度は有効期間（startDate〜endDate）と入力期間（inputStartDate〜inputEndDate）を持つ。
 * 入力期間は年度期間と独立しており、棚卸の入力受付期間として使用する。
 */
@Service
@RequiredArgsConstructor
public class FiscalYearService {

    private final FiscalYearRepository fiscalYearRepository;
    private final FiscalYearSettingsRepository settingsRepository;

    /** 年度を新規作成する。入力期間（inputStartDate / inputEndDate）は省略可能（null 許容）。 */
    @Transactional
    public FiscalYear createFiscalYear(String name, LocalDate startDate, LocalDate endDate,
                                       LocalDate inputStartDate, LocalDate inputEndDate) {
        FiscalYear fy = FiscalYear.create(name, startDate, endDate, inputStartDate, inputEndDate);
        return fiscalYearRepository.save(fy);
    }

    /**
     * 年度情報を更新する。active フラグで年度の有効・無効を切り替えられる。
     * 対象年度が存在しない場合は 404 をスローする。
     */
    @Transactional
    public FiscalYear updateFiscalYear(int id, String name, LocalDate startDate, LocalDate endDate,
                                       LocalDate inputStartDate, LocalDate inputEndDate, boolean active) {
        FiscalYear fy = fiscalYearRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        fy.update(name, startDate, endDate, inputStartDate, inputEndDate, active);
        return fiscalYearRepository.save(fy);
    }

    /**
     * 年度設定（年度開始月）を更新する。
     * FiscalYearSettings はシングルトン（id=1 の1件のみ）のため、findById(1) で取得する。
     */
    @Transactional
    public FiscalYearSettings updateSettings(short fiscalYearStartMonth) {
        // id=1 のシングルトンレコードを取得する（存在しない場合は初期化漏れのため 404 をスローする）
        FiscalYearSettings s = settingsRepository.findById((short) 1)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        s.setFiscalYearStartMonth(fiscalYearStartMonth);
        return settingsRepository.save(s);
    }
}
