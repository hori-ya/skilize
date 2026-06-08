/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * マスタExcel出力・取込APIコントローラー。ADMIN専用。
 * ITスキル・資格・ADセミナーの各マスタをExcelでダウンロード／アップロードする。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.presentation;

import com.skilize.master.application.MasterExcelService;
import com.skilize.master.application.query.MasterImportQueryResult;
import com.skilize.master.infrastructure.excel.ExcelFormatException;
import com.skilize.master.presentation.response.MasterImportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * マスタ Excel 出力・取込 API。ADMIN 専用。
 * 出力: GET /api/master-excel/{target}/download → xlsx バイナリ
 * 取込: POST /api/master-excel/{target}/upload  → MasterImportResponse or EXCEL_IMPORT_ERROR
 */
@Slf4j
@RestController
@RequestMapping("/api/master-excel")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class MasterExcelController {

    private static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    /** 最大ファイルサイズ: 10MB */
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final MasterExcelService masterExcelService;

    // ─── ITスキルマスタ ──────────────────────────────────────────────────────────

    /**
     * ITスキルマスタをExcelファイルとしてダウンロードする。
     *
     * @return Excelファイル（.xlsx）のレスポンス
     */
    @GetMapping("/it-skills/download")
    public ResponseEntity<byte[]> downloadItSkills() {
        log.info("ITスキルマスタ Excel 出力");
        byte[] bytes = masterExcelService.exportItSkillExcel();
        return excelResponse(bytes, "ItSkillMaster.xlsx");
    }

    /**
     * ITスキルマスタExcelファイルを取り込む。バリデーションエラーがあれば400エラーを返す。
     *
     * @param file アップロードされたExcelファイル（.xlsx）
     * @return 取込成功時は MasterImportResponse、エラー時は EXCEL_IMPORT_ERROR
     */
    @PostMapping(value = "/it-skills/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadItSkills(@RequestParam("file") MultipartFile file) {
        validateUploadFile(file);
        log.info("ITスキルマスタ Excel 取込: filename={}", file.getOriginalFilename());
        MasterImportQueryResult result = masterExcelService.importItSkillExcel(file);
        return toImportResponse(result);
    }

    // ─── 参考資格マスタ ──────────────────────────────────────────────────────────

    /**
     * 資格マスタをExcelファイルとしてダウンロードする。
     *
     * @return Excelファイル（.xlsx）のレスポンス
     */
    @GetMapping("/qualifications/download")
    public ResponseEntity<byte[]> downloadQualifications() {
        log.info("参考資格マスタ Excel 出力");
        byte[] bytes = masterExcelService.exportQualificationExcel();
        return excelResponse(bytes, "QualificationMaster.xlsx");
    }

    /**
     * 資格マスタExcelファイルを取り込む。バリデーションエラーがあれば400エラーを返す。
     *
     * @param file アップロードされたExcelファイル（.xlsx）
     * @return 取込成功時は MasterImportResponse、エラー時は EXCEL_IMPORT_ERROR
     */
    @PostMapping(value = "/qualifications/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadQualifications(@RequestParam("file") MultipartFile file) {
        validateUploadFile(file);
        log.info("参考資格マスタ Excel 取込: filename={}", file.getOriginalFilename());
        MasterImportQueryResult result = masterExcelService.importQualificationExcel(file);
        return toImportResponse(result);
    }

    // ─── ADマスタ ────────────────────────────────────────────────────────────────

    /**
     * ADセミナーマスタをExcelファイルとしてダウンロードする。
     *
     * @return Excelファイル（.xlsx）のレスポンス
     */
    @GetMapping("/ad-seminars/download")
    public ResponseEntity<byte[]> downloadAdSeminars() {
        log.info("ADマスタ Excel 出力");
        byte[] bytes = masterExcelService.exportAdSeminarExcel();
        return excelResponse(bytes, "AdSeminarMaster.xlsx");
    }

    /**
     * ADセミナーマスタExcelファイルを取り込む。バリデーションエラーがあれば400エラーを返す。
     *
     * @param file アップロードされたExcelファイル（.xlsx）
     * @return 取込成功時は MasterImportResponse、エラー時は EXCEL_IMPORT_ERROR
     */
    @PostMapping(value = "/ad-seminars/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAdSeminars(@RequestParam("file") MultipartFile file) {
        validateUploadFile(file);
        log.info("ADマスタ Excel 取込: filename={}", file.getOriginalFilename());
        MasterImportQueryResult result = masterExcelService.importAdSeminarExcel(file);
        return toImportResponse(result);
    }

    // ─── ヘルパー ────────────────────────────────────────────────────────────────

    /**
     * アップロードファイルの事前検証。
     * サイズ上限（10MB）と拡張子（.xlsx のみ）をチェックし、不正なら ExcelFormatException をスローする。
     */
    private void validateUploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ExcelFormatException("EXCEL_FILE_EMPTY");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ExcelFormatException("EXCEL_FILE_TOO_LARGE");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new ExcelFormatException("EXCEL_INVALID_FORMAT");
        }
    }

    private ResponseEntity<byte[]> excelResponse(byte[] bytes, String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(bytes);
    }

    /** バリデーションエラーがあれば 400 EXCEL_IMPORT_ERROR を返し、なければ 200 MasterImportResponse を返す。 */
    private ResponseEntity<?> toImportResponse(MasterImportQueryResult result) {
        if (result.hasErrors()) {
            List<Map<String, Object>> errorList = result.errors().stream()
                    .map(e -> Map.<String, Object>of(
                            "sheet", e.sheet(),
                            "row", e.row(),
                            "column", e.column(),
                            "message", e.message()))
                    .toList();
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "EXCEL_IMPORT_ERROR",
                    "message", "取込データに誤りがあります",
                    "errors", errorList));
        }
        return ResponseEntity.ok(MasterImportResponse.from(result));
    }
}
