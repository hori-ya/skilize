/**************************************************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * リクエストの Authorization ヘッダーから JWT を取り出して検証し、
 * SecurityContext に認証情報をセットするフィルター。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.shared.infrastructure;

import com.skilize.user.domain.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * リクエストごとに JWT を検証して SecurityContext に認証情報をセットするフィルター。
 * 検証失敗時は SecurityContext をクリアして次のフィルターへ委譲する（401 応答はエントリーポイントが返す）。
 * フィルターチェーン上の位置: UsernamePasswordAuthenticationFilter の前に配置する。
 *
 * OncePerRequestFilter: サーブレットのフォワード・インクルード等でフィルターが二重実行されることを防ぐ
 * Spring 標準基底クラス。通常の @Component フィルターでは複数回実行される可能性がある。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        // "Bearer " の7文字を除いてトークン本体を取り出す
        String token = authHeader.substring(7);
        try {
            String userIdStr = jwtUtil.extractUserId(token);
            // SecurityContext に認証済みオブジェクトがない場合のみ処理する
            // （同一リクエスト内で複数のフィルターが走っても二重セットを防ぐ）
            if (userIdStr != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                userRepository.findById(Integer.valueOf(userIdStr)).ifPresent(user -> {
                    // is_active=false のユーザーは JWT が有効でもブロックする（アカウント無効化の即時反映）
                    if (user.isActive() && jwtUtil.isTokenValid(token)) {
                        // credentials（第2引数）は JWT 検証済みのため null。第3引数に権限リストを渡す。
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        // リクエストの IP アドレス・セッション ID を認証オブジェクトに付与（監査ログ用）
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        // SecurityContextHolder はスレッドローカルな認証ストア。
                        // ここにセットした情報がコントローラー・@PreAuthorize から参照される。
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        // 認証成功後に MDC の userId を更新する（LoggingFilter が "-" でセット済み）
                        MDC.put(LoggingFilter.MDC_USER_ID, String.valueOf(user.getId()));
                    }
                });
            }
        } catch (Exception e) {
            // トークンの解析・検証で例外が起きた場合は認証なしとして扱い、次フィルターへ委譲する
            log.warn("JWT validation failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
