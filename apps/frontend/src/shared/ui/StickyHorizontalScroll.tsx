import { useRef, useEffect, type ReactNode } from 'react';

interface Props {
  children: ReactNode;
  className?: string;
}

export default function StickyHorizontalScroll({ children, className }: Props) {
  const innerRef = useRef<HTMLDivElement>(null);
  const barRef = useRef<HTMLDivElement>(null);
  const phantomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const inner = innerRef.current;
    const bar = barRef.current;
    const phantom = phantomRef.current;
    if (!inner || !bar || !phantom) return;

    const update = () => {
      phantom.style.width = `${inner.scrollWidth}px`;
      bar.style.display = inner.scrollWidth > inner.clientWidth ? 'block' : 'none';
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

  return (
    <div className="sticky-h-scroll">
      <div ref={innerRef} className={`sticky-h-scroll__inner${className ? ` ${className}` : ''}`}>
        {children}
      </div>
      <div ref={barRef} className="sticky-h-scroll__bar">
        <div ref={phantomRef} className="sticky-h-scroll__phantom" />
      </div>
    </div>
  );
}
