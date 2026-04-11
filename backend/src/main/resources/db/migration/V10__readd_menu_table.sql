CREATE TABLE menus (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parent_id UUID REFERENCES menus(id) ON DELETE CASCADE,
    program_id UUID REFERENCES programs(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_visible BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE menus IS 'UI 렌더링용 네비게이션 트리 (접근권한은 program_id로 검증)';

CREATE TRIGGER update_menus_updated_at BEFORE UPDATE ON menus FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 초기 데이터 세팅법 (기존 programs 연결)
-- 시스템 관리 폴더 (최상단)
INSERT INTO menus (id, name, sort_order) VALUES ('00000000-0000-0000-0000-000000000100', '시스템 관리', 900);
-- 하위 메뉴들 연결
INSERT INTO menus (parent_id, program_id, name, sort_order) 
SELECT '00000000-0000-0000-0000-000000000100', id, '권한 마스터 관리', 10 FROM programs WHERE program_code = 'PG_SYS_AUTH';

-- 상품목록 폴더 없는 단일 메뉴
INSERT INTO menus (program_id, name, sort_order) 
SELECT id, '자재 마켓', 100 FROM programs WHERE program_code = 'PG_MKT_LIST';

-- 주문/배차 단일 메뉴
INSERT INTO menus (program_id, name, sort_order) 
SELECT id, '주문/배차 내역', 200 FROM programs WHERE program_code = 'PG_ORDER_LIST';

-- 대시보드 단일 메뉴
INSERT INTO menus (parent_id, program_id, name, sort_order)
SELECT '00000000-0000-0000-0000-000000000100', id, '대시보드', 0 FROM programs WHERE program_code = 'PG_DASH_SUM';
