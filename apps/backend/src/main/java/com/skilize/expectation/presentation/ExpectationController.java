package com.skilize.expectation.presentation;

import com.skilize.expectation.application.ExpectationService;
import com.skilize.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/expectations")
@RequiredArgsConstructor
public class ExpectationController {

    private final ExpectationService expectationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public ExpectationResponse get(@PathVariable int userId,
                                    @AuthenticationPrincipal User requester) {
        return expectationService.getForUser(userId, requester);
    }

    @PutMapping("/tl")
    @PreAuthorize("hasRole('TL')")
    public ExpectationResponse saveTl(@PathVariable int userId,
                                       @AuthenticationPrincipal User requester,
                                       @RequestBody SaveExpectationRequest req) {
        return expectationService.saveTlExpectation(userId, requester, req.expectation());
    }

    @PutMapping("/company")
    @PreAuthorize("hasRole('ADMIN')")
    public ExpectationResponse saveCompany(@PathVariable int userId,
                                            @AuthenticationPrincipal User requester,
                                            @RequestBody SaveExpectationRequest req) {
        return expectationService.saveCompanyExpectation(userId, requester, req.expectation());
    }
}
