/**************************************************************************************************************
 * 機能ID      ：FY
 * 機能名      ：年度管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 年度・年度設定の REST API コントローラー。
 * 一覧・現在年度取得は全ロール参照可。作成・更新・設定変更は ADMIN のみ実行可能。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.fiscalyear.presentation;

import com.skilize.fiscalyear.application.FiscalYearService;
import com.skilize.fiscalyear.presentation.request.FiscalYearRequest;
import com.skilize.fiscalyear.presentation.request.FiscalYearSettingsRequest;
import com.skilize.fiscalyear.presentation.response.FiscalYearResponse;
import com.skilize.fiscalyear.presentation.response.FiscalYearSettingsResponse;
import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.fiscalyear.domain.model.FiscalYearSettings;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 年度・年度設定の REST API コントローラー。
 * 一覧・現在年度取得は全ロール参照可。作成・更新・設定変更は ADMIN のみ。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FiscalYearController {

    private final FiscalYearService fiscalYearService;

    /** 全年度を開始日降順（新しい順）で返す。ロール制限なし。 */
    @GetMapping("/fiscal-years")
    public List<FiscalYearResponse> list() {
        List<FiscalYearResponse> responses = new ArrayList<>();
        for (FiscalYear fiscalYear : fiscalYearService.findAllOrderByStartDateDesc()) {
            responses.add(FiscalYearResponse.from(fiscalYear));
        }
        return responses;
    }

    /** 今日の日付を基準に現在有効な年度を返す。存在しない場合は 404 を返す。 */
    @GetMapping("/fiscal-years/current")
    public ResponseEntity<FiscalYearResponse> current() {
        // findCurrent() は today が startDate〜endDate に含まれる年度を返す
        Optional<FiscalYear> fiscalYearOptional = fiscalYearService.findCurrent(LocalDate.now());
        if (fiscalYearOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(FiscalYearResponse.from(fiscalYearOptional.get()));
    }

    /** 年度を新規作成する（ADMIN のみ）。日付文字列は ISO-8601 形式（"yyyy-MM-dd"）で受け取り LocalDate に変換する。 */
    @PostMapping("/fiscal-years")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FiscalYearResponse> create(@Valid @RequestBody FiscalYearRequest req) {
        // 入力期間は省略可能（null の場合は制限なし扱い）
        LocalDate inputStartDate = null;
        if (req.inputStartDate() != null) {
            inputStartDate = LocalDate.parse(req.inputStartDate());
        }
        LocalDate inputEndDate = null;
        if (req.inputEndDate() != null) {
            inputEndDate = LocalDate.parse(req.inputEndDate());
        }
        FiscalYear saved = fiscalYearService.createFiscalYear(
                req.name(),
                // LocalDate.parse() は ISO-8601 形式の文字列を LocalDate に変換する
                LocalDate.parse(req.startDate()),
                LocalDate.parse(req.endDate()),
                inputStartDate,
                inputEndDate
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(FiscalYearResponse.from(saved));
    }

    /**
     * 年度情報を更新する（ADMIN のみ）。
     * active フラグが未送信の場合は現在の値を維持する（部分更新パターン。Service側で解決する）。
     */
    @PutMapping("/fiscal-years/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public FiscalYearResponse update(@PathVariable int id, @Valid @RequestBody FiscalYearRequest req) {
        LocalDate inputStartDate = null;
        if (req.inputStartDate() != null) {
            inputStartDate = LocalDate.parse(req.inputStartDate());
        }
        LocalDate inputEndDate = null;
        if (req.inputEndDate() != null) {
            inputEndDate = LocalDate.parse(req.inputEndDate());
        }
        FiscalYear updated = fiscalYearService.updateFiscalYear(
                id,
                req.name(),
                LocalDate.parse(req.startDate()),
                LocalDate.parse(req.endDate()),
                inputStartDate,
                inputEndDate,
                req.active()
        );
        return FiscalYearResponse.from(updated);
    }

    /** 年度設定（年度開始月）を取得する。ロール制限なし。 */
    @GetMapping("/fiscal-year-settings")
    public FiscalYearSettingsResponse getSettings() {
        FiscalYearSettings s = fiscalYearService.getSettings();
        return new FiscalYearSettingsResponse(s.getFiscalYearStartMonth());
    }

    /** 年度設定（年度開始月）を更新する（ADMIN のみ）。fiscalYearStartMonth は 1〜12 の範囲で指定する。 */
    @PutMapping("/fiscal-year-settings")
    @PreAuthorize("hasRole('ADMIN')")
    public FiscalYearSettingsResponse updateSettings(@Valid @RequestBody FiscalYearSettingsRequest req) {
        FiscalYearSettings s = fiscalYearService.updateSettings(req.fiscalYearStartMonth());
        return new FiscalYearSettingsResponse(s.getFiscalYearStartMonth());
    }

}
