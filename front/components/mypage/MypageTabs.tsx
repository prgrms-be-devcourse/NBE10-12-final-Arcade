'use client';

import { MYPAGE_TABS, type MypageTabKey } from '@/lib/mypageTabs';

interface MypageTabsProps {
  active: MypageTabKey;
  onChange: (key: MypageTabKey) => void;
}

export function MypageTabs({ active, onChange }: MypageTabsProps) {
  return (
    <div className="mypage-tabs" role="tablist">
      {MYPAGE_TABS.map((tab) => (
        <button
          key={tab.key}
          type="button"
          className={tab.key === active ? 'tab-btn is-active' : 'tab-btn'}
          role="tab"
          aria-selected={tab.key === active}
          onClick={() => onChange(tab.key)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}
