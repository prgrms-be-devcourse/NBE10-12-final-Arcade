'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { Icon } from '@/components/icons/Icon';
import type { HeroSlide } from '@/lib/types';

const AUTO_PLAY_MS = 6000;

/** 홈 메인 배너 캐러셀 */
export function HeroSlider({ slides }: { slides: HeroSlide[] }) {
  const [index, setIndex] = useState(0);
  const total = slides.length;

  const go = useCallback((next: number) => setIndex(((next % total) + total) % total), [total]);

  useEffect(() => {
    if (total <= 1) return;
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduced) return;
    const timer = window.setInterval(() => setIndex((prev) => (prev + 1) % total), AUTO_PLAY_MS);
    return () => window.clearInterval(timer);
  }, [total]);

  if (total === 0) return null;

  return (
    <section className="hero container">
      <div
        className="hero-slider"
        role="region"
        aria-roledescription="carousel"
        aria-label="크루온 메인 배너"
        tabIndex={0}
        onKeyDown={(event) => {
          if (event.key === 'ArrowLeft') go(index - 1);
          if (event.key === 'ArrowRight') go(index + 1);
        }}
      >
        <div className="hero-floor" aria-hidden="true" />
        <div className="hero-scrim" aria-hidden="true" />

        <div className="hero-slides">
          {slides.map((slide, slideIndex) => (
            <article
              key={slide.id}
              className={slideIndex === index ? 'hero-slide is-active' : 'hero-slide'}
            >
              <div className="hero-slide-text">
                {slide.tag ? <span className="slide-tag">{slide.tag}</span> : null}
                <HeroHeadline slide={slide} primary={slideIndex === 0} />
                <p className="hero-sub">{slide.sub}</p>
                <div className="hero-actions">
                  {slide.actions.map((action) => (
                    <Link
                      key={action.label}
                      href={action.href}
                      className={`btn ${action.variant === 'primary' ? 'btn-primary' : 'btn-ghost'}`}
                    >
                      {action.label}
                    </Link>
                  ))}
                </div>
              </div>

              <div className="hero-art" aria-hidden="true">
                <div className="mark">{slide.art.mark}</div>
                <div className="sub">{slide.art.sub}</div>
                <div className="hero-art-chips">
                  {slide.art.chips.map((chip) => (
                    <span key={chip} className="hero-chip">
                      {chip}
                    </span>
                  ))}
                </div>
              </div>
            </article>
          ))}
        </div>

        <button
          type="button"
          className="slider-arrow prev"
          aria-label="이전 슬라이드"
          onClick={() => go(index - 1)}
        >
          <Icon name="i-chevron-left" />
        </button>
        <button
          type="button"
          className="slider-arrow next"
          aria-label="다음 슬라이드"
          onClick={() => go(index + 1)}
        >
          <Icon name="i-chevron-right" />
        </button>

        <div className="slider-footer">
          <span className="slider-count">
            {String(index + 1).padStart(2, '0')} / {String(total).padStart(2, '0')}
          </span>
          <div className="slider-dots" role="tablist" aria-label="슬라이드 선택">
            {slides.map((slide, dotIndex) => (
              <button
                key={slide.id}
                type="button"
                className={dotIndex === index ? 'dot is-active' : 'dot'}
                role="tab"
                aria-selected={dotIndex === index}
                aria-label={`${dotIndex + 1}번 슬라이드`}
                onClick={() => go(dotIndex)}
              />
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

/** 헤드라인의 {glow} 자리에 강조 색을 입힌다 */
function HeroHeadline({ slide, primary }: { slide: HeroSlide; primary: boolean }) {
  const [before, after] = slide.headline.split('{glow}');
  const content = slide.headlineGlow ? (
    <>
      {before}
      <span className="glow">{slide.headlineGlow}</span>
      {after}
    </>
  ) : (
    slide.headline
  );

  return primary ? (
    <h1 className="hero-headline">{content}</h1>
  ) : (
    <p className="hero-headline">{content}</p>
  );
}
