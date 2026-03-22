-- V7__insert_default_menus.sql
-- Root Menus
INSERT INTO menus (id, menu_code, name, parent_id, sort_order, is_visible, created_at) VALUES 
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'MENU_PROD_LIST', '상품목록', NULL, 10, TRUE, CURRENT_TIMESTAMP),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'MENU_PROD_REG', '상품등록', NULL, 20, TRUE, CURRENT_TIMESTAMP),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'MENU_ADM_ORDER', '주문 관리', NULL, 30, TRUE, CURRENT_TIMESTAMP),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'MENU_ADM_USER', '사용자 관리', NULL, 40, TRUE, CURRENT_TIMESTAMP),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'MENU_ADM_MENU', '메뉴 관리', NULL, 50, TRUE, CURRENT_TIMESTAMP),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', 'MENU_ADM_ROLE', '권한 관리', NULL, 60, TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (menu_code) DO NOTHING;

-- Assign Read permission to ADMIN/DEVELOPER role for all menus by default
-- Use ON CONFLICT to avoid duplicate key errors if some permissions already exist
INSERT INTO role_menu_actions (id, role_id, menu_id, can_read, can_create, can_update, can_delete, can_excel)
SELECT 
    gen_random_uuid(), 
    r.id, 
    m.id, 
    TRUE, TRUE, TRUE, TRUE, TRUE
FROM roles r, menus m
WHERE r.name IN ('ADMIN', 'DEVELOPER')
ON CONFLICT (role_id, menu_id) DO NOTHING;
