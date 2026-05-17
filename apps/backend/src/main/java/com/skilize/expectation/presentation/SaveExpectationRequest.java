package com.skilize.expectation.presentation;

/** 期待コメント保存リクエスト。TL期待（PUT /tl）・会社期待（PUT /company）の両エンドポイントで共用する。 */
public record SaveExpectationRequest(String expectation) {}
