-- '프로그램 관리' 또는 '화면 등록 관리' 메뉴 추가
-- '시스템관리' 메뉴 하위에 추가함. 
-- 시스템관리 ID: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380000' (V13__build_system_management_hierarchy.sql에서 조회 필요)
-- 또는 그냥 하드코딩된 '메뉴 관리' 등의 부모를 찾아서 넣기.
-- V12에서는 시스템관리라는 부모가 명시적으로 없었을 수도 있음. 
-- V13에서 '시스템관리'를 root로 만들었으므로 그 아래에 넣겠음.

INSERT INTO menus (id, name, parent_id, sort_order, is_visible, path, program_id)
SELECT 
    uuid_generate_v4(), 
    '프로그램 관리', 
    (SELECT id FROM menus WHERE name = '시스템관리' LIMIT 1),
    70, 
    TRUE, 
    '/admin/programs',
    (SELECT id FROM programs WHERE program_code = 'PG_SYS_AUTH' LIMIT 1);
