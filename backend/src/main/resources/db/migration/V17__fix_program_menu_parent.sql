-- 프로그램 관리 메뉴의 부모를 '시스템 관리' (공백 포함)로 정확히 지정
-- 만약 이미 존재한다면 수정, 없다면 신규 생성
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM menus WHERE name = '프로그램 관리') THEN
        UPDATE menus 
        SET parent_id = (SELECT id FROM menus WHERE name = '시스템 관리' LIMIT 1)
        WHERE name = '프로그램 관리';
    ELSE
        INSERT INTO menus (id, name, parent_id, sort_order, is_visible, path, program_id)
        VALUES (
            uuid_generate_v4(), 
            '프로그램 관리', 
            (SELECT id FROM menus WHERE name = '시스템 관리' LIMIT 1),
            70, 
            TRUE, 
            '/admin/programs',
            (SELECT id FROM programs WHERE program_code = 'PG_SYS_AUTH' LIMIT 1)
        );
    END IF;
END $$;
