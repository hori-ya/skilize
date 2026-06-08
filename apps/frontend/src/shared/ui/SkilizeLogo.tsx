/*******************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * Skilize アプリのロゴ SVG コンポーネント。NavBar やログインページで使用する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
interface Props {
  size?: number;
}

/**
 * Skilize ロゴの SVG コンポーネント。
 *
 * 棒グラフ風のアイコンを表示する。size プロパティで表示サイズを指定できる。
 */
export default function SkilizeLogo({ size = 24 }: Props) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <rect x="1"  y="15" width="6" height="8"  rx="1.5" fill="currentColor" opacity="0.35" />
      <rect x="9"  y="8"  width="6" height="15" rx="1.5" fill="currentColor" opacity="0.65" />
      <rect x="17" y="1"  width="6" height="22" rx="1.5" fill="currentColor" />
    </svg>
  );
}
