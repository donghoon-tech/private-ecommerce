-- 1. Drop old matrix and tree tables
DROP TABLE IF EXISTS role_menu_actions CASCADE;
DROP TABLE IF EXISTS menus CASCADE;
DROP TABLE IF EXISTS role_permissions CASCADE;
DROP TABLE IF EXISTS permissions CASCADE;

-- 2. Create Single Table programs
CREATE TABLE programs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category1 VARCHAR(50),
    category2 VARCHAR(50),
    program_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    url VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('WEB', 'API')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE programs IS '통합 프로그램 마스터 (WEB, API)';

-- 3. Create mapping table
CREATE TABLE role_programs (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    program_id UUID NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, program_id)
);

-- 4. Insert Seed Data
INSERT INTO programs (category1, category2, program_code, name, url, type) VALUES 
('대시보드', '업무요약', 'PG_DASH_SUM', '업무 요약 대시보드', 'admin/dash/summary', 'WEB'),
('자재마켓', '상품 검색/조회', 'PG_MKT_LIST', '상품 검색 및 조회', 'market/product/list', 'WEB'),
('주문/배차', '주문내역', 'PG_ORDER_LIST', '주문 내역 관리', 'order/history/list', 'WEB'),
('시스템관리', '권한설정관리', 'PG_SYS_AUTH', '권한 설정 매트릭스', 'admin/system/auth', 'WEB'),
(NULL, NULL, 'API_DASH_STATS', '대시보드 통계 API', '/api/v1/dash/stats', 'API'),
(NULL, NULL, 'API_MKT_SEARCH', '마켓 상품 검색 API', '/api/v1/market/search', 'API'),
(NULL, NULL, 'API_AUTH_SYNC', 'RBAC 권한 동기화 API', '/api/v1/internal/rbac/sync', 'API'),
(NULL, NULL, 'API_ORDER_DETAIL', '주문 상세 정보 조회 API', '/api/v1/order/detail', 'API');

-- 5. Seed mapping for UNVERIFIED
INSERT INTO role_programs (role_id, program_id) 
SELECT r.id, p.id FROM roles r, programs p WHERE r.name = 'UNVERIFIED' AND p.program_code IN ('API_MKT_SEARCH');

-- 6. Seed mapping for USER
INSERT INTO role_programs (role_id, program_id) 
SELECT r.id, p.id FROM roles r, programs p WHERE r.name = 'USER' AND p.program_code IN ('PG_MKT_LIST', 'API_MKT_SEARCH', 'PG_ORDER_LIST', 'API_ORDER_DETAIL');

-- 7. Seed mapping for ADMIN & DEVELOPER
INSERT INTO role_programs (role_id, program_id) 
SELECT r.id, p.id FROM roles r, programs p WHERE r.name IN ('ADMIN', 'DEVELOPER');
