import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import commonJa from './locales/ja/common.json';
import navJa from './locales/ja/nav.json';
import authJa from './locales/ja/auth.json';
import inventoryJa from './locales/ja/inventory.json';
import teamJa from './locales/ja/team.json';
import masterJa from './locales/ja/master.json';
import aiSupportJa from './locales/ja/ai-support.json';
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
        team: teamJa,
        master: masterJa,
        'ai-support': aiSupportJa,
        errors: errorsJa,
      },
    },
    interpolation: {
      escapeValue: false,
    },
  });

export default i18n;
