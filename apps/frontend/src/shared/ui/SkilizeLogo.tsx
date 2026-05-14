interface Props {
  size?: number;
}

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
