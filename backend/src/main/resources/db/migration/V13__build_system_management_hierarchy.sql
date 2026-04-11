-- 시스템 관리(부모 라우팅용 폴더) 추가
INSERT INTO menus (id, name, parent_id, sort_order, is_visible, path) 
VALUES ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b11', '시스템 관리', NULL, 100, TRUE, '#');

-- 새로 만든 '시스템 관리' 폴더 아래로 '메뉴 관리'와 '권한 관리' 이동
UPDATE menus 
SET parent_id = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b11'
WHERE name IN ('메뉴 관리', '권한 관리');
