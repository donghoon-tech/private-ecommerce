-- 이전에 잘못 들어간 메뉴들 모두 삭제 (안전하게 비우기)
DELETE FROM menus;

-- 메뉴에 menu_code 컬럼이 필요하다면 여기에 추가 (기존 V7 데이터 호환성)
-- 하지만 프론트엔드가 path 기반으로 변경되었으므로 path에 각각의 라우팅 주소를 넣고
-- UI에 표시될 이름과 정렬 순서를 1dff60a 커밋의 V7__insert_default_menus.sql과 정확히 일치시킵니다.

-- 메뉴 데이터 복원 (V7과 동일한 6개 구조)
INSERT INTO menus (id, name, parent_id, sort_order, is_visible, path) VALUES 
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '상품목록', NULL, 10, TRUE, '/'),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', '상품등록', NULL, 20, TRUE, '/product/register'),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', '주문 관리', NULL, 30, TRUE, '/admin/orders'),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', '사용자 관리', NULL, 40, TRUE, '/admin/users'),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', '메뉴 관리', NULL, 50, TRUE, '/admin/menus'),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', '권한 관리', NULL, 60, TRUE, '/admin/roles');

-- 보안 맵핑 (Program) 연결
-- 상품목록, 등록 -> 자재 마켓 프로그램 권한 (PG_MKT_LIST)
UPDATE menus SET program_id = (SELECT id FROM programs WHERE program_code = 'PG_MKT_LIST') 
WHERE name IN ('상품목록', '상품등록');

-- 주문 관리 -> 주문 내역 관리 프로그램 권한 (PG_ORDER_LIST)
UPDATE menus SET program_id = (SELECT id FROM programs WHERE program_code = 'PG_ORDER_LIST') 
WHERE name IN ('주문 관리');

-- 시스템(사용자/메뉴/권한) 관리 -> 시스템 관리 프로그램 권한 (PG_SYS_AUTH)
UPDATE menus SET program_id = (SELECT id FROM programs WHERE program_code = 'PG_SYS_AUTH') 
WHERE name IN ('사용자 관리', '메뉴 관리', '권한 관리');
