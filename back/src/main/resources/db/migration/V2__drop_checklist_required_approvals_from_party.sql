-- Party.checklistRequiredApprovals 필드 제거에 따른 컬럼 정리.
-- IF EXISTS를 둘 다 붙여서, 테이블이 아직 없는 새 환경(ddl-auto가 나중에 만듦)이나
-- 이미 수동으로 컬럼을 지운 환경에서도 에러 없이 통과하도록 한다.
ALTER TABLE IF EXISTS party DROP COLUMN IF EXISTS checklist_required_approvals;
