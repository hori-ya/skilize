package com.skilize.expectation.presentation;

import com.skilize.expectation.application.ExpectationService;
import com.skilize.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * ユーザーへの期待情報の REST API コントローラー。
 * TL期待（/tl）は担当TLのみ、会社期待（/company）はADMINのみ更新可。参照は TL/ADMIN。
 * 詳細なアクセス制御ロジックは ExpectationService に委譲している。
 */
@RestController
@RequestMapping("/api/users/{userId}/expectations")
@RequiredArgsConstructor
public class ExpectationController {

    private final ExpectationService expectationService;

    /**
     * 指定ユーザーへの期待情報（TL期待・会社期待）を取得する（TL/ADMIN のみ）。
     * TL は担当メンバーのみ参照可。レコード未作成の場合は空のレスポンスを返す（404 ではない）。
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public ExpectationResponse get(@PathVariable int userId,
                                    @AuthenticationPrincipal User requester) {
        return expectationService.getForUser(userId, requester);
    }

    /**
     * TL期待コメントを保存する（担当TLのみ）。
     * 担当TLでない場合は Service 層で 403 をスローする。
     */
    @PutMapping("/tl")
    @PreAuthorize("hasRole('TL')")
    public ExpectationResponse saveTl(@PathVariable int userId,
                                       @AuthenticationPrincipal User requester,
                                       @RequestBody SaveExpectationRequest req) {
        return expectationService.saveTlExpectation(userId, requester, req.expectation());
    }

    /** 会社期待コメントを保存する（ADMIN のみ）。 */
    @PutMapping("/company")
    @PreAuthorize("hasRole('ADMIN')")
    public ExpectationResponse saveCompany(@PathVariable int userId,
                                            @AuthenticationPrincipal User requester,
                                            @RequestBody SaveExpectationRequest req) {
        return expectationService.saveCompanyExpectation(userId, requester, req.expectation());
    }
}
