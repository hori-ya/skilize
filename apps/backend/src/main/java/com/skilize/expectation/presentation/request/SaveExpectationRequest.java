package com.skilize.expectation.presentation.request;

/**
 * 期待コメント保存リクエスト。TL期待（PUT /api/users/{userId}/expectation/tl）・
 * 会社期待（PUT /api/users/{userId}/expectation/company）の両エンドポイントで共用する。
 *
 * @param expectation 期待コメント本文（null または空文字で削除）
 */
public record SaveExpectationRequest(String expectation) {}
