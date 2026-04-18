-- 프론트엔드의 메뉴 네비게이션이 DB의 programs.url 을 1:1로 정확히 따라가도록 개선
-- 1. 메뉴별로 고유한 WEB 프로그램을 생성
-- 2. 각 메뉴와 새 프로그램 매핑
-- 3. menus 테이블의 하드코딩된 path를 모두 NULL로 초기화
-- 4. 새 프로그램에 대한 역할 권한 부여

-- 1. 신규 프로그램 생성
INSERT INTO programs (id, category1, category2, program_code, name, url, type) VALUES 
(uuid_generate_v4(), '자재마켓', '상품 등록', 'PG_MKT_REG', '상품 등록 화면', '/product/register', 'WEB'),
(uuid_generate_v4(), '시스템관리', '사용자 관리', 'PG_SYS_USER', '사용자 관리 화면', '/admin/users', 'WEB'),
(uuid_generate_v4(), '시스템관리', '권한 관리', 'PG_SYS_ROLE', '권한 관리 화면', '/admin/roles', 'WEB'),
(uuid_generate_v4(), '시스템관리', '메뉴 관리', 'PG_SYS_MENU', '메뉴 관리 화면', '/admin/menus', 'WEB'),
(uuid_generate_v4(), '시스템관리', '프로그램 관리', 'PG_SYS_PROG', '프로그램 관리 화면', '/admin/programs', 'WEB');

-- 2. 권한 부여 (DEVELOPER, ADMIN에게 모든 신규 프로그램 권한 부여)
INSERT INTO role_programs (role_id, program_id)
SELECT r.id, p.id 
FROM roles r, programs p 
WHERE r.name IN ('ADMIN', 'DEVELOPER')
  AND p.program_code IN ('PG_MKT_REG', 'PG_SYS_USER', 'PG_SYS_ROLE', 'PG_SYS_MENU', 'PG_SYS_PROG');

-- USER 그룹에게는 상품 등록(PG_MKT_REG) 권한 부여
INSERT INTO role_programs (role_id, program_id)
SELECT r.id, p.id 
FROM roles r, programs p 
WHERE r.name = 'USER'
  AND p.program_code = 'PG_MKT_REG';

-- 3. 메뉴와 프로그램 매핑 변경 및 path 초기화
-- 3-1. 자재 마켓
UPDATE menus 
SET program_id = (SELECT id FROM programs WHERE program_code = 'PG_MKT_REG' LIMIT 1),
    path = NULL
WHERE name = '상품등록';

UPDATE menus 
SET path = NULL
WHERE name = '상품목록' AND program_id = (SELECT id FROM programs WHERE program_code = 'PG_MKT_LIST' LIMIT 1);

-- 3-2. 주문 관리
UPDATE menus 
SET path = NULL
WHERE name = '주문 관리' AND program_id = (SELECT id FROM programs WHERE program_code = 'PG_ORDER_LIST' LIMIT 1);

-- 3-3. 시스템 관리 (사용자, 메뉴, 권한, 프로그램)
UPDATE menus 
SET program_id = (SELECT id FROM programs WHERE program_code = 'PG_SYS_USER' LIMIT 1),
    path = NULL
WHERE name = '사용자 관리';

UPDATE menus 
SET program_id = (SELECT id FROM programs WHERE program_code = 'PG_SYS_ROLE' LIMIT 1),
    path = NULL
WHERE name = '권한 관리';

UPDATE menus 
SET program_id = (SELECT id FROM programs WHERE program_code = 'PG_SYS_MENU' LIMIT 1),
    path = NULL
WHERE name = '메뉴 관리';

UPDATE menus 
SET program_id = (SELECT id FROM programs WHERE program_code = 'PG_SYS_PROG' LIMIT 1),
    path = NULL
WHERE name = '프로그램 관리' OR name = '화면 등록 관리';

-- 3-4. 기타 자식 메뉴가 존재하는 폴더형 메뉴들의 path 도 NULL (클릭 시 링크 없음을 명확히 함)
UPDATE menus 
SET path = NULL 
WHERE name IN ('자재마켓', '시스템관리', '시스템 관리');

