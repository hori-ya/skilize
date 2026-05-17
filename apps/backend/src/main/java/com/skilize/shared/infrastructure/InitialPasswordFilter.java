package com.skilize.shared.infrastructure;

import com.skilize.user.domain.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        // `instanceof User user` はJava16以降のパターンマッチング。
        // 型チェックとキャストを1行で行い、以降のブロックで `user` 変数として使える。
        // getPrincipal() が User でない場合（匿名アクセス等）はフィルタースキップ。
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User user)) {
            chain.doFilter(request, response);
            return;
        }

        if (user.isInitialPassword() && !ALLOWED_PATHS.contains(request.getServletPath())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(BODY);
            return;
        }

        chain.doFilter(request, response);
    }
}
