-- V29: 상품 목록 경로 구체화 (/ -> /market/product/list)
-- 1. 프로그램 URL을 구체적인 경로로 수정
UPDATE programs 
SET url = '/market/product/list' 
WHERE program_code = 'PG_MKT_LIST';

-- 2. 메뉴의 path를 NULL로 설정하여 프로그램의 URL을 자동으로 참조하도록 함
UPDATE menus 
SET path = NULL 
WHERE id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
