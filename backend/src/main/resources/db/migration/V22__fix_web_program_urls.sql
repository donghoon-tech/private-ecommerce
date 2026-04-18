-- V22: WEB 타입 프로그램의 URL을 실제 Vue 라우터 경로에 맞게 정규화
-- DynamicAuthorizationManager(BE)처럼 FE도 DB에서 URL→programCode 매핑을 읽어 동적 라우트 가드를 구성하기 위함.
-- AntPath 스타일 패턴 지원 (예: /admin/**)

-- 상품 검색/조회 화면 (홈 = /)
UPDATE programs SET url = '/'
WHERE program_code = 'PG_MKT_LIST' AND type = 'WEB';

-- 주문 내역 관리 화면
UPDATE programs SET url = '/admin/orders'
WHERE program_code = 'PG_ORDER_LIST' AND type = 'WEB';

-- 시스템 관리 전체 (/admin/** 하위 화면 통합 커버)
-- admin/users, admin/roles, admin/menus, admin/programs 등 모두 포함
UPDATE programs SET url = '/admin/**'
WHERE program_code = 'PG_SYS_AUTH' AND type = 'WEB';

-- 대시보드 (아직 실제 Vue 라우트 없음 - 나중을 위해 경로 정규화만)
UPDATE programs SET url = '/admin/dashboard'
WHERE program_code = 'PG_DASH_SUM' AND type = 'WEB';
