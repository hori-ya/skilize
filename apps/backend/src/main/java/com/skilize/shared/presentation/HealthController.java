package com.skilize.shared.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ヘルスチェックエンドポイント。nginx やロードバランサーが生死確認に使用する。
 * 認証不要（SecurityConfig で permitAll() 設定済み）。
 */
@RestController
public class HealthController {

    /** {"status": "ok"} を返す。サーバーが起動していれば常に 200 を返す。 */
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
