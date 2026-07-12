/**************************************************************************************************************
 * 機能ID      ：RPT
 * 機能名      ：帳票・レポート
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 帳票・レポート機能のプレゼンテーション層。棚卸PDF帳票（JasperReports）のダウンロードエンドポイントを提供する。
 * 認証済みユーザーが自分の棚卸、またはTL/ADMINが担当ユーザーの棚卸を PDF でダウンロードできる。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.report.presentation;

import com.skilize.report.application.ReportService;
import com.skilize.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 帳票・レポート機能のREST APIコントローラー。棚卸PDF帳票のダウンロードエンドポイントを提供する。
 * アクセス制御（本人のみ、またはTL/ADMIN）はReportServiceのcheckAccessで行う。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventories")
public class ReportController {

    private final ReportService reportService;

    /**
     * 指定棚卸のPDF帳票をダウンロードする。
     * 本人またはTL/ADMINのみ取得可。アクセス不可の場合はReportService内で例外をスローする。
     *
     * @param id   棚卸内部ID
     * @param user 認証済みユーザー
     * @return PDFバイナリ（Content-Disposition: attachment）
     */
    @GetMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadInventoryReport(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "user") User user) {
        byte[] pdf = reportService.generateInventoryReport(id, user);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("inventory_report_" + id + ".pdf")
                        .build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
