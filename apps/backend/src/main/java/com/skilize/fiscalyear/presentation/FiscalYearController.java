package com.skilize.fiscalyear.presentation;

import com.skilize.fiscalyear.application.FiscalYearService;
import com.skilize.fiscalyear.dto.*;
import com.skilize.fiscalyear.domain.FiscalYear;
import com.skilize.fiscalyear.domain.FiscalYearRepository;
import com.skilize.fiscalyear.domain.FiscalYearSettings;
import com.skilize.fiscalyear.domain.FiscalYearSettingsRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * 年度・年度設定の REST API コントローラー。
 * 一覧・現在年度取得は全ロール参照可。作成・更新・設定変更は ADMIN のみ。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FiscalYearController {

    private final FiscalYearService fiscalYearService;
    private final FiscalYearRepository fiscalYearRepository;
    private final FiscalYearSettingsRepository settingsRepository;

    /** 全年度を開始日降順（新しい順）で返す。ロール制限なし。 */
    @GetMapping("/fiscal-years")
    public List<FiscalYearDto> list() {
        return fiscalYearRepository.findAll().stream()
                // 開始日の降順（新しい年度が先頭）でソートする
                .sorted((a, b) -> b.getStartDate().compareTo(a.getStartDate()))
                .map(FiscalYearDto::from)
                .toList();
    }

    /** 今日の日付を基準に現在有効な年度を返す。存在しない場合は 404 を返す。 */
    @GetMapping("/fiscal-years/current")
    public ResponseEntity<FiscalYearDto> current() {
        // findCurrent() は today が startDate〜endDate に含まれる年度を返す
        return fiscalYearRepository.findCurrent(LocalDate.now())
                .map(FiscalYearDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** 年度を新規作成する（ADMIN のみ）。日付文字列は ISO-8601 形式（"yyyy-MM-dd"）で受け取り LocalDate に変換する。 */
    @PostMapping("/fiscal-years")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FiscalYearDto> create(@Valid @RequestBody FiscalYearRequest req) {
        FiscalYear saved = fiscalYearService.createFiscalYear(
                req.name(),
                // LocalDate.parse() は ISO-8601 形式の文字列を LocalDate に変換する
                LocalDate.parse(req.startDate()),
                LocalDate.parse(req.endDate()),
                // 入力期間は省略可能（null の場合は制限なし扱い）
                req.inputStartDate() != null ? LocalDate.parse(req.inputStartDate()) : null,
                req.inputEndDate() != null ? LocalDate.parse(req.inputEndDate()) : null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(FiscalYearDto.from(saved));
    }

    /**
     * 年度情報を更新する（ADMIN のみ）。
     * active フラグが未送信の場合は現在の値を維持する（部分更新パターン）。
     */
    @PutMapping("/fiscal-years/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public FiscalYearDto update(@PathVariable int id, @Valid @RequestBody FiscalYearRequest req) {
        // active が未送信（null）の場合、既存の値をそのまま引き継ぐ
        FiscalYear existing = fiscalYearRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        FiscalYear updated = fiscalYearService.updateFiscalYear(
                id,
                req.name(),
                LocalDate.parse(req.startDate()),
                LocalDate.parse(req.endDate()),
                req.inputStartDate() != null ? LocalDate.parse(req.inputStartDate()) : null,
                req.inputEndDate() != null ? LocalDate.parse(req.inputEndDate()) : null,
                req.active() != null ? req.active() : existing.isActive()
        );
        return FiscalYearDto.from(updated);
    }

    /** 年度設定（年度開始月）を取得する。ロール制限なし。 */
    @GetMapping("/fiscal-year-settings")
    public FiscalYearSettingsDto getSettings() {
        // FiscalYearSettings はシングルトン（id=1 のみ）
        FiscalYearSettings s = settingsRepository.findById((short) 1)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new FiscalYearSettingsDto(s.getFiscalYearStartMonth());
    }

    /** 年度設定（年度開始月）を更新する（ADMIN のみ）。fiscalYearStartMonth は 1〜12 の範囲で指定する。 */
    @PutMapping("/fiscal-year-settings")
    @PreAuthorize("hasRole('ADMIN')")
    public FiscalYearSettingsDto updateSettings(@Valid @RequestBody FiscalYearSettingsRequest req) {
        FiscalYearSettings s = fiscalYearService.updateSettings(req.fiscalYearStartMonth());
        return new FiscalYearSettingsDto(s.getFiscalYearStartMonth());
    }

}
