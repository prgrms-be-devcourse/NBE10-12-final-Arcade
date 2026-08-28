'use client';

import { useState } from 'react';
import { Icon } from '@/components/icons/Icon';
import { EmailVerifyField } from './EmailVerifyField';
import {
  FormGroup,
  FormRow,
  Options,
  SelectField,
  TextField,
} from '@/components/ui/Field';
import { searchCompanies, verifyBusinessNumber } from '@/lib/api';
import type { HostCompany } from '@/lib/types';

const ORG_TYPES = ['기업', '공공기관', '대학 · 학과', '동아리', '협회 · 단체', '기타'] as const;

/** 주최측 회원가입 전용 — 회사 검색/연결 또는 신규 등록(사업자 진위확인 포함) */
export function CompanySetup() {
  const [keyword, setKeyword] = useState('');
  const [results, setResults] = useState<HostCompany[]>([]);
  const [selected, setSelected] = useState<HostCompany | null>(null);
  const [newFormOpen, setNewFormOpen] = useState(false);

  const [companyName, setCompanyName] = useState('');
  const [orgType, setOrgType] = useState<string>(ORG_TYPES[0]);
  const [ceoName, setCeoName] = useState('');
  const [bizNumber, setBizNumber] = useState('');
  const [bizState, setBizState] = useState<'idle' | 'ok' | 'error'>('idle');
  const [bizMessage, setBizMessage] = useState('국세청 사업자등록정보 진위확인 API로 조회합니다.');
  const [companyEmail, setCompanyEmail] = useState('');
  const [homepage, setHomepage] = useState('');

  const search = async () => {
    const found = (await searchCompanies(keyword)) as HostCompany[];
    setResults(found);
  };

  const verifyBiz = async () => {
    const result = await verifyBusinessNumber({ bizNumber, companyName, ceoName });
    setBizState(result.verified ? 'ok' : 'error');
    setBizMessage(result.message);
  };

  return (
    <div>
      <FormGroup
        label="회사 · 기관 설정"
        hint="이미 등록된 회사를 검색해 연결하거나, 없으면 새로 등록하세요. 공모전은 연결된 회사 이름으로 게시됩니다."
      >
        {selected ? (
          <div className="company-selected">
            <span className="company-logo">{selected.name.charAt(0)}</span>
            <span className="company-meta">
              <span className="cname">
                {selected.name}
                {selected.verified ? <span className="verify-badge">인증됨</span> : null}
              </span>
              <span className="csub">기업 · 사업자등록번호 {selected.bizNumber}</span>
            </span>
            <button type="button" className="company-change" onClick={() => setSelected(null)}>
              변경
            </button>
          </div>
        ) : (
          <>
            <div className="company-search">
              <TextField
                placeholder="회사 · 기관명 검색 (예: 코드잼)"
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    event.preventDefault();
                    search();
                  }
                }}
              />
              <button type="button" className="btn btn-ghost" onClick={search}>
                검색
              </button>
            </div>
            {results.length > 0 ? (
              <div className="company-result-list">
                {results.map((company) => (
                  <button
                    key={company.id}
                    type="button"
                    className="company-result"
                    onClick={() => {
                      setSelected(company);
                      setNewFormOpen(false);
                    }}
                  >
                    <span className="company-logo">{company.name.charAt(0)}</span>
                    <span className="company-meta">
                      <span className="cname">{company.name}</span>
                      <span className="csub">사업자등록번호 {company.bizNumber}</span>
                    </span>
                  </button>
                ))}
              </div>
            ) : null}
          </>
        )}

        {!selected ? (
          <button
            type="button"
            className="add-row-btn"
            style={{ marginTop: '0.75rem' }}
            onClick={() => setNewFormOpen((value) => !value)}
          >
            <Icon name="i-plus" />
            찾는 회사가 없어요 · 새로 등록하기
          </button>
        ) : null}

        {newFormOpen && !selected ? (
          <div className="company-new-form">
            <FormGroup label="회사 · 기관명">
              <TextField
                placeholder="사업자등록증 상 상호명"
                value={companyName}
                onChange={(event) => setCompanyName(event.target.value)}
              />
            </FormGroup>
            <FormRow>
              <FormGroup label="기관 유형">
                <SelectField value={orgType} onChange={(event) => setOrgType(event.target.value)}>
                  <Options values={ORG_TYPES} />
                </SelectField>
              </FormGroup>
              <FormGroup label="대표자">
                <TextField
                  placeholder="사업자등록증 상 대표자명"
                  value={ceoName}
                  onChange={(event) => setCeoName(event.target.value)}
                />
              </FormGroup>
            </FormRow>
            <FormGroup label="사업자등록번호">
              <div className="biz-verify-row">
                <TextField
                  placeholder="000-00-00000"
                  maxLength={12}
                  value={bizNumber}
                  onChange={(event) => setBizNumber(event.target.value)}
                />
                <button type="button" className="btn btn-ghost" onClick={verifyBiz}>
                  사업자번호 확인
                </button>
              </div>
              <p className="biz-status" data-state={bizState}>
                {bizMessage}
              </p>
            </FormGroup>

            <EmailVerifyField
              label="회사 대표(공용) 이메일"
              placeholder="contact@company.co.kr"
              idleMessage="공모전 문의가 이 주소로 전달돼요. 도메인 소유 확인을 위해 인증이 필요합니다."
              value={companyEmail}
              onChange={setCompanyEmail}
            />

            <FormGroup label="공식 홈페이지">
              <TextField
                type="url"
                placeholder="https://"
                value={homepage}
                onChange={(event) => setHomepage(event.target.value)}
              />
            </FormGroup>

            <p className="company-note">
              상호명 · 대표자 · 사업자등록번호가 국세청 정보와 모두 일치해야 인증이 완료됩니다.
              진위확인을 통과하면 관리자 확인 없이 인증 배지가 바로 붙어요.
            </p>
          </div>
        ) : null}
      </FormGroup>

      <FormRow>
        <FormGroup label="담당 부서">
          <TextField placeholder="예: 인재영입팀" />
        </FormGroup>
        <FormGroup label="직함">
          <TextField placeholder="예: 채용 담당자" />
        </FormGroup>
      </FormRow>
    </div>
  );
}
