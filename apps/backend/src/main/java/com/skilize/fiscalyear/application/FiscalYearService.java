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

import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.fiscalyear.domain.repository.FiscalYearRepository;
import com.skilize.fiscalyear.domain.model.FiscalYearSettings;
import com.skilize.fiscalyear.domain.repository.FiscalYearSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    /** 全年度を開始日降順（新しい順）で返す。 */
    @Transactional(readOnly = true)
    public List<FiscalYear> findAllOrderByStartDateDesc() {
        List<FiscalYear> fiscalYears = new ArrayList<>(fiscalYearRepository.findAll());
        fiscalYears.sort(new Comparator<FiscalYear>() {
            @Override
            public int compare(FiscalYear a, FiscalYear b) {
                return b.getStartDate().compareTo(a.getStartDate());
            }
        });
        return fiscalYears;
    }

    /** 今日の日付を基準に現在有効な年度を取得する。存在しない場合は Optional.empty() を返す。 */
    @Transactional(readOnly = true)
    public Optional<FiscalYear> findCurrent(LocalDate today) {
        return fiscalYearRepository.findCurrent(today);
    }

    /** 年度設定（年度開始月）を取得する。FiscalYearSettings はシングルトン（id=1 の1件のみ）。 */
    @Transactional(readOnly = true)
    public FiscalYearSettings getSettings() {
        Optional<FiscalYearSettings> settingsOptional = settingsRepository.findById((short) 1);
        if (settingsOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return settingsOptional.get();
    }

    /** 年度を新規作成する。入力期間（inputStartDate / inputEndDate）は省略可能（null 許容）。 */
    @Transactional
    public FiscalYear createFiscalYear(String name, LocalDate startDate, LocalDate endDate,
                                       LocalDate inputStartDate, LocalDate inputEndDate) {
        FiscalYear fy = FiscalYear.create(name, startDate, endDate, inputStartDate, inputEndDate);
        return fiscalYearRepository.save(fy);
    }

    /**
     * 年度情報を更新する。active フラグで年度の有効・無効を切り替えられる。
     * active が null の場合は現在の値を維持する（部分更新パターン）。
     * 対象年度が存在しない場合は 404 をスローする。
     */
    @Transactional
    public FiscalYear updateFiscalYear(int id, String name, LocalDate startDate, LocalDate endDate,
                                       LocalDate inputStartDate, LocalDate inputEndDate, Boolean active) {
        Optional<FiscalYear> fiscalYearOptional = fiscalYearRepository.findById(id);
        if (fiscalYearOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        FiscalYear fy = fiscalYearOptional.get();
        boolean resolvedActive;
        if (active != null) {
            resolvedActive = active;
        } else {
            resolvedActive = fy.isActive();
        }
        fy.update(name, startDate, endDate, inputStartDate, inputEndDate, resolvedActive);
        return fiscalYearRepository.save(fy);
    }

    /**
     * 年度設定（年度開始月）を更新する。
     * FiscalYearSettings はシングルトン（id=1 の1件のみ）のため、findById(1) で取得する。
     */
    @Transactional
    public FiscalYearSettings updateSettings(short fiscalYearStartMonth) {
        // id=1 のシングルトンレコードを取得する（存在しない場合は初期化漏れのため 404 をスローする）
        Optional<FiscalYearSettings> settingsOptional = settingsRepository.findById((short) 1);
        if (settingsOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        FiscalYearSettings s = settingsOptional.get();
        s.setFiscalYearStartMonth(fiscalYearStartMonth);
        return settingsRepository.save(s);
    }
}
