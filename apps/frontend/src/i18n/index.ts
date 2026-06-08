/*******************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * i18next の初期化モジュール。日本語リソースを各 namespace ごとに登録する。
 * アプリ起動時に main.tsx からインポートされ、全コンポーネントで翻訳が利用可能になる。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import commonJa from './locales/ja/common.json';
import navJa from './locales/ja/nav.json';
import authJa from './locales/ja/auth.json';
import inventoryJa from './locales/ja/inventory.json';
import userJa from './locales/ja/user.json';
import masterJa from './locales/ja/master.json';
import aiJa from './locales/ja/ai.json';
import errorsJa from './locales/ja/errors.json';

i18n
  .use(initReactI18next)
  .init({
    lng: 'ja',
    fallbackLng: 'ja',
    resources: {
      ja: {
        common: commonJa,
        nav: navJa,
        auth: authJa,
        inventory: inventoryJa,
        user: userJa,
        master: masterJa,
        ai: aiJa,
        errors: errorsJa,
      },
    },
    interpolation: {
      escapeValue: false,
    },
  });

export default i18n;
