/*******************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * 画面下部に固定表示される水平スクロールバーを提供するコンポーネント。
 * テーブルなど横幅が広いコンテンツでスクロールバーを常に表示するために使用する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { useRef, useEffect, type ReactNode } from 'react';

interface Props {
  children: ReactNode;
  className?: string;
}

/**
 * 画面下部に固定表示される水平スクロールバーコンポーネント。
 *
 * コンテンツの横幅がウィンドウを超える場合にのみスクロールバーを表示し、
 * コンテンツとスクロールバーの位置を同期する。
 */
export default function StickyHorizontalScroll({ children, className }: Props) {
  const innerRef = useRef<HTMLDivElement>(null);
  const barRef = useRef<HTMLDivElement>(null);
  const phantomRef = useRef<HTMLDivElement>(null);

  // 内部コンテンツのリサイズを監視してスクロールバーの表示・非表示を切り替え、双方向同期を設定する
  useEffect(() => {
    const inner = innerRef.current;
    const bar = barRef.current;
    const phantom = phantomRef.current;
    if (!inner || !bar || !phantom) return;

    const update = () => {
      phantom.style.width = `${inner.scrollWidth}px`;
      if (inner.scrollWidth > inner.clientWidth) {
        bar.style.display = 'block';
      } else {
        bar.style.display = 'none';
      }
    };

    const syncToInner = () => {
      if (inner.scrollLeft !== bar.scrollLeft) inner.scrollLeft = bar.scrollLeft;
    };
    const syncToBar = () => {
      if (bar.scrollLeft !== inner.scrollLeft) bar.scrollLeft = inner.scrollLeft;
    };

    const ro = new ResizeObserver(update);
    ro.observe(inner);

    bar.addEventListener('scroll', syncToInner, { passive: true });
    inner.addEventListener('scroll', syncToBar, { passive: true });
    update();

    return () => {
      ro.disconnect();
      bar.removeEventListener('scroll', syncToInner);
      inner.removeEventListener('scroll', syncToBar);
    };
  }, []);

  let innerClassName = 'sticky-h-scroll__inner';
  if (className) {
    innerClassName += ` ${className}`;
  }

  return (
    <div className="sticky-h-scroll">
      <div ref={innerRef} className={innerClassName}>
        {children}
      </div>
      <div ref={barRef} className="sticky-h-scroll__bar">
        <div ref={phantomRef} className="sticky-h-scroll__phantom" />
      </div>
    </div>
  );
}
