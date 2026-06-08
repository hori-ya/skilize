/*******************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * ページ先頭へスクロールするフローティングボタン。
 * スクロール量が 200px を超えたときに表示される。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { useState, useEffect } from 'react';

/**
 * ページ先頭へスクロールするフローティングボタン。
 *
 * スクロール量が 200px を超えると表示され、クリックでスムーズスクロールする。
 */
export default function ScrollToTopButton() {
  const [visible, setVisible] = useState(false);

  // スクロール量が 200px を超えたときにボタンを表示する
  useEffect(() => {
    const onScroll = () => setVisible(window.scrollY > 200);
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  return (
    <button
      className={`scroll-top-btn${visible ? '' : ' scroll-top-btn--hidden'}`}
      onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
      aria-label="ページ先頭に戻る"
    >
      ↑
    </button>
  );
}
