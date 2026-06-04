package com.skilize.report.presentation;

import com.skilize.report.application.ReportService;
import com.skilize.user.domain.User;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventories")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadInventoryReport(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
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
