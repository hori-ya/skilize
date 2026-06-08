/*******************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * Axios エラーを処理するユーティリティ関数群。
 * バックエンドのエラーコードを i18n の翻訳テキストに変換して返す。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import i18n from '../../i18n';

interface ApiErrorBody {
  code?: string;
  message?: string;
  errors?: Array<{ field: string; message: string }>;
}

/**
 * Axios エラーからエラーコードを取得する。
 * バックエンドは { code: "ERROR_CODE" } 形式でエラーを返す。
 */
export function getApiErrorCode(error: unknown): string {
  const body = extractBody(error);
  return body?.code ?? 'INTERNAL_ERROR';
}

/**
 * エラーコードを errors namespace の翻訳テキストに変換して返す。
 * 対応するコードが未定義の場合はコードをそのまま返す。
 */
export function getApiErrorMessage(error: unknown): string {
  const code = getApiErrorCode(error);
  const key = `errors:${code}`;
  const translated = i18n.t(key);
  return translated !== key ? translated : code;
}

/**
 * バリデーションエラーのフィールドごとのメッセージを取得する。
 * 各 field.message はアノテーション名（"NotBlank", "Size" 等）のコードとして返される。
 */
export function getValidationErrors(error: unknown): Array<{ field: string; message: string }> {
  const body = extractBody(error);
  if (!body?.errors) return [];
  return body.errors.map(e => ({
    field: e.field,
    message: i18n.t(`errors:${e.message}`, { defaultValue: e.message }),
  }));
}

function extractBody(error: unknown): ApiErrorBody | null {
  if (error && typeof error === 'object' && 'response' in error) {
    const res = (error as { response?: { data?: ApiErrorBody } }).response;
    return res?.data ?? null;
  }
  return null;
}
