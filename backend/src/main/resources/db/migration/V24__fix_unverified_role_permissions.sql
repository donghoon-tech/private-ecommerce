-- V24: UNVERIFIED 역할에 상품 목록 조회 권한 추가 및 API 경로 정정 
-- 1. API_MKT_SEARCH 프로그램 URL 정정 (기존 /api/v1/market/search -> /api/products)
UPDATE programs SET url = '/api/products' WHERE program_code = 'API_MKT_SEARCH';

-- 2. UNVERIFIED 역할에 PG_MKT_LIST (WEB 메뉴 권한) 추가
-- 이를 통해 미인증 사용자도 홈화면(상품목록) 메뉴를 보고 접근할 수 있게 함
INSERT INTO role_programs (role_id, program_id)
SELECT r.id, p.id 
FROM roles r, programs p 
WHERE r.name = 'UNVERIFIED' 
  AND p.program_code = 'PG_MKT_LIST'
ON CONFLICT DO NOTHING;
