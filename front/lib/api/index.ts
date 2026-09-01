/**
 * API 진입점.
 *
 * 화면 컴포넌트는 항상 여기서 함수를 import 한다.
 *   import { fetchParties } from '@/lib/api';
 *
 * 실제 서버 연동이 시작되면 각 모듈의 `USE_MOCK` 분기만 걷어내면 된다.
 */
export * from './client';
export * from './admin';
export * from './auth';
export * from './chat';
export * from './contests';
export * from './exhibitions';
export * from './goals';
export * from './home';
export * from './host';
export * from './messages';
export * from './notifications';
export * from './parties';
export * from './teams';
export * from './todos';
export * from './users';
