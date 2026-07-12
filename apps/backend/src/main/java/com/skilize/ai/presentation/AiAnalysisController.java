/**************************************************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * AI キャリア分析結果のプレゼンテーション層。分析結果の参照エンドポイントを提供する。
 * 分析トリガーは棚卸提出時（InventoryCompletedEventListener）に行われ、このコントローラーは参照のみ担う。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.ai.presentation;

import com.skilize.ai.application.AiAnalysisService;
import com.skilize.ai.application.query.AiAnalysisQueryResult;
import com.skilize.shared.domain.exception.AuthException;
import com.skilize.user.application.UserService;
import com.skilize.user.domain.model.Role;
import com.skilize.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * AI キャリア分析結果の REST API コントローラー。
 * 分析のトリガーはここではなく棚卸提出時（InventoryCompletedEventListener）に行われる。
 * このコントローラーは既存の分析結果を参照するのみ。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;
    private final UserService userService;

    /** 自分の AI 分析結果一覧を返す（全ロール参照可）。 */
    @GetMapping("/users/me/ai-analyses")
    public List<AiAnalysisQueryResult> getMyAnalyses(@AuthenticationPrincipal(expression = "user") User user) {
        return aiAnalysisService.findByUserId(user.getId());
    }

    /**
     * 指定ユーザーの AI 分析結果一覧を返す（TL/ADMIN のみ）。
     * TL は担当チームメンバーのみ参照可（他チームへのアクセスは 403）。
     */
    @GetMapping("/users/{userId}/ai-analyses")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public List<AiAnalysisQueryResult> getMemberAnalyses(
            @PathVariable int userId,
            @AuthenticationPrincipal(expression = "user") User currentUser) {
        User targetUser = userService.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません"));

        // TL は担当チームメンバー（tl_user_id が自分のID のユーザー）のみ参照可
        if (currentUser.getRole() == Role.TL) {
            if (!currentUser.getId().equals(targetUser.getTlUserId())) {
                throw new AuthException("FORBIDDEN", "");
            }
        }

        return aiAnalysisService.findByUserId(userId);
    }
}
