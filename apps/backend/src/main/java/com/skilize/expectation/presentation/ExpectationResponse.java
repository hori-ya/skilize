package com.skilize.expectation.presentation;

import com.skilize.expectation.domain.UserExpectation;

public record ExpectationResponse(
        String tlExpectation,
        String companyExpectation
) {
    public static ExpectationResponse from(UserExpectation e) {
        return new ExpectationResponse(e.getTlExpectation(), e.getCompanyExpectation());
    }

    public static ExpectationResponse empty() {
        return new ExpectationResponse(null, null);
    }
}
