package com.skilize.user.domain;

/**
 * ユーザーロール。
 * GENERAL = 一般ユーザー（自分の棚卸のみ操作可）
 * TL      = チームリーダー（担当チームメンバーの棚卸を参照・面談メモ記入可）
 * ADMIN   = 管理者（全ユーザー・全マスタ・年度設定の管理可）
 */
public enum Role {
    GENERAL, TL, ADMIN
}
