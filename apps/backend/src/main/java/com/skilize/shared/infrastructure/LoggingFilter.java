/**************************************************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * リクエストごとに MDC（Mapped Diagnostic Context）を初期化するフィルター。
 * requestId（UUID）と userId を全ログに自動付与し、リクエスト終了後にクリアする。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.shared.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * リクエストごとに MDC（Mapped Diagnostic Context）を初期化するフィルター。
 * requestId（UUID）と userId を全ログに自動付与し、リクエスト終了後にクリアする。
 * userId は初期値 "-"（未認証）として設定し、JwtAuthenticationFilter が認証後に上書きする。
 */
@Component
public class LoggingFilter extends OncePerRequestFilter {

    static final String MDC_REQUEST_ID = "requestId";
    static final String MDC_USER_ID = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        MDC.put(MDC_REQUEST_ID, UUID.randomUUID().toString());
        MDC.put(MDC_USER_ID, "-");
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
