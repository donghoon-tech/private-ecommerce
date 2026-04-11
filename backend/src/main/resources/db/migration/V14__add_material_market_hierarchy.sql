-- '자재마켓' 상위 폴더 추가
INSERT INTO menus (id, name, parent_id, sort_order, is_visible, path) 
VALUES ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b22', '자재마켓', NULL, 10, TRUE, '#');

-- '상품목록'과 '상품등록'을 '자재마켓' 하위로 이동
UPDATE menus 
SET parent_id = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b22'
WHERE name IN ('상품목록', '상품등록');

-- 기존 수평 구조였던 순서 조정
UPDATE menus SET sort_order = 1 WHERE name = '상품목록';
UPDATE menus SET sort_order = 2 WHERE name = '상품등록';
UPDATE menus SET sort_order = 100 WHERE name = '시스템 관리';
