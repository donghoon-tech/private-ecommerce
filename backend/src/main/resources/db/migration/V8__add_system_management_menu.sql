-- V8__add_system_management_menu.sql

-- 1. Create the new root menu '시스템관리'
INSERT INTO menus (id, menu_code, name, parent_id, sort_order, is_visible, created_at)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17', 'MENU_SYS_MNG', '시스템관리', NULL, 90, TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (menu_code) DO NOTHING;

-- 2. Update '메뉴 관리' and '권한 관리' to be children of '시스템관리'
UPDATE menus
SET parent_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17',
    sort_order = 10
WHERE menu_code = 'MENU_ADM_MENU';

UPDATE menus
SET parent_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17',
    sort_order = 20
WHERE menu_code = 'MENU_ADM_ROLE';

-- 3. Assign full permissions to ADMIN and DEVELOPER for the new '시스템관리' menu
INSERT INTO role_menu_actions (id, role_id, menu_id, can_read, can_create, can_update, can_delete, can_excel)
SELECT 
    gen_random_uuid(), 
    r.id, 
    m.id, 
    TRUE, TRUE, TRUE, TRUE, TRUE
FROM roles r, menus m
WHERE r.name IN ('ADMIN', 'DEVELOPER')
  AND m.menu_code = 'MENU_SYS_MNG'
ON CONFLICT (role_id, menu_id) DO NOTHING;
