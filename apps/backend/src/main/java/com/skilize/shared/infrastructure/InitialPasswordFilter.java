/**************************************************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 初回ログイン時のパスワード変更を強制するフィルター。
 * is_initial_password=true のユーザーは認証系エンドポイント以外へのアクセスを 403 でブロックする。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.shared.infrastructure;

import com.skilize.user.infrastructure.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 初回ログイン強制パスワード変更フィルター。
 * is_initial_password=true のユーザーは ALLOWED_PATHS 以外のリクエストをすべて 403 でブロックする。
 * JwtAuthenticationFilter の直後に配置されるため、認証済みユーザーのみが対象となる。
 */
@Slf4j
@Component
public class InitialPasswordFilter extends OncePerRequestFilter {

    // 初期パスワード保持中でもアクセスを許可するパス
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/change-password",
            "/api/auth/logout",
            "/api/auth/me"
    );

    private static final String BODY =
            "{\"code\":\"INITIAL_PASSWORD_REQUIRED\",\"message\":\"初期パスワードの変更が必要です\"}";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // `instanceof UserPrincipal principal` はJava16以降のパターンマッチング。
        // 型チェックとキャストを1行で行い、以降のブロックで `principal` 変数として使える。
        // getPrincipal() が UserPrincipal でない場合（匿名アクセス等）はフィルタースキップ。
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            chain.doFilter(request, response);
            return;
        }

        if (principal.getUser().isInitialPassword() && !ALLOWED_PATHS.contains(request.getServletPath())) {
            log.warn("Initial password change required. Blocked path={}", request.getServletPath());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(BODY);
            return;
        }

        chain.doFilter(request, response);
    }
}
