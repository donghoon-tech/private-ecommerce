-- PostgreSQL DDL for 가설재/유로폼 B2B 플랫폼

-- UUID 확장 활성화
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- 1. 권한 및 역할 관리 (RBAC Core)
-- ============================================================

-- 시스템 프그램 및 권한 리소스 테이블 (단일 테이블 구조: WEB/API 통합)
CREATE TABLE programs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category1 VARCHAR(50), -- 대분류 (WEB용, API는 NULL)
    category2 VARCHAR(50), -- 중분류 (WEB용, API는 NULL)
    program_code VARCHAR(50) NOT NULL UNIQUE, -- 프로그램ID (PG_DASH_SUM, API_DASH_STATS 등)
    name VARCHAR(100) NOT NULL, -- 프로그램명
    url VARCHAR(255) NOT NULL, -- 프로그램 URL
    type VARCHAR(20) NOT NULL CHECK (type IN ('WEB', 'API')), -- 프로그램 유형
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE programs IS '통합 프로그램 마스터 (WEB, API)';

-- 역할 테이블: 권한들의 묶음 (미인증회원, 정회원, 운영자 등)
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE roles IS '사용자 역할 (예: UNVERIFIED, USER, ADMIN)';

-- 역할별 프로그램 할당 테이블 (N:M)
CREATE TABLE role_programs (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    program_id UUID NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, program_id)
);

-- ============================================================
-- 2. 사용자 관련 테이블
-- ============================================================

-- 사용자 테이블 (기존 role ENUM 삭제 후 role_id FK 도입)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role_id UUID NOT NULL REFERENCES roles(id), -- RBAC 역할 참조
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    name VARCHAR(100) NOT NULL,
    representative_phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(255),
    business_number VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    phone_verified_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    failed_login_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_role_id ON users(role_id);
CREATE INDEX idx_users_phone ON users(representative_phone);
CREATE INDEX idx_users_username ON users(username);

-- 사업자 프로필 테이블
CREATE TABLE business_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    business_name VARCHAR(200) NOT NULL,
    business_number VARCHAR(20) NOT NULL,
    representative_name VARCHAR(100) NOT NULL,
    office_address TEXT NOT NULL,
    storage_address TEXT,
    br_image_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    rejection_reason TEXT,
    approved_at TIMESTAMPTZ,
    approved_by UUID REFERENCES users(id),
    is_main BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_business_profiles_user_id ON business_profiles(user_id);
CREATE INDEX idx_business_profiles_status ON business_profiles(status);

-- 배송지 관리 테이블
CREATE TABLE delivery_addresses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    address_name VARCHAR(100) NOT NULL,
    full_address TEXT NOT NULL,
    detail_address TEXT,
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 3. 상품 및 카테고리 (비즈니스 로직)
-- ============================================================

CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parent_id UUID REFERENCES categories(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    depth INT NOT NULL CHECK (depth IN (0, 1, 2)),
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    seller_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id),
    item_name VARCHAR(200) NOT NULL,
    item_condition VARCHAR(20) NOT NULL CHECK (item_condition IN ('신재', '고재')),
    unit_price DECIMAL(15, 2) NOT NULL CHECK (unit_price >= 0),
    sale_unit VARCHAR(50),
    stock_quantity INT NOT NULL CHECK (stock_quantity >= 0),
    total_amount DECIMAL(15, 2) NOT NULL CHECK (total_amount >= 0),
    loading_address TEXT NOT NULL,
    loading_address_display VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected', 'selling', 'sold_out')),
    rejection_reason TEXT,
    approved_at TIMESTAMPTZ,
    approved_by UUID REFERENCES users(id),
    is_displayed BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE product_images (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 4. 주문 및 장바구니
-- ============================================================

CREATE TABLE cart_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    seller_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quantity INT NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, product_id)
);

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    buyer_id UUID NOT NULL REFERENCES users(id),
    seller_id UUID NOT NULL REFERENCES users(id),
    order_type VARCHAR(20) NOT NULL CHECK (order_type IN ('platform', 'phone')),
    truck_tonnage VARCHAR(20),
    truck_type VARCHAR(20) CHECK (truck_type IN ('cargo', 'wingbody')),
    shipping_loading_address TEXT NOT NULL,
    shipping_unloading_address TEXT NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL CHECK (total_amount >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'shipping', 'delivered', 'completed')),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (payment_status IN ('pending', 'confirmed', 'settled')),
    order_memo TEXT,
    admin_memo TEXT,
    delivery_started_at TIMESTAMPTZ,
    delivery_completed_at TIMESTAMPTZ,
    carrier_info TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    product_name_snapshot VARCHAR(200) NOT NULL,
    product_condition_snapshot VARCHAR(20) NOT NULL,
    price_snapshot DECIMAL(15, 2) NOT NULL CHECK (price_snapshot >= 0),
    quantity INT NOT NULL CHECK (quantity > 0),
    subtotal DECIMAL(15, 2) NOT NULL CHECK (subtotal >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE order_images (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    uploaded_by UUID NOT NULL REFERENCES users(id),
    image_url TEXT NOT NULL,
    image_type VARCHAR(20) NOT NULL CHECK (image_type IN ('loading', 'delivery')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 5. 알림 서비스
-- ============================================================

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    order_id UUID REFERENCES orders(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('sms', 'email')),
    content TEXT NOT NULL,
    sent_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'sent', 'failed')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 6. 트리거: updated_at 자동 업데이트
-- ============================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_business_profiles_updated_at BEFORE UPDATE ON business_profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_delivery_addresses_updated_at BEFORE UPDATE ON delivery_addresses FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_products_updated_at BEFORE UPDATE ON products FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_cart_items_updated_at BEFORE UPDATE ON cart_items FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 7. 초기 시드 데이터 (권한 및 역할)
-- ============================================================

-- 1) 프로그램 데이터 삽입 (WEB 및 API 통합)
INSERT INTO programs (category1, category2, program_code, name, url, type) VALUES 
('대시보드', '업무요약', 'PG_DASH_SUM', '업무 요약 대시보드', 'admin/dash/summary', 'WEB'),
('자재마켓', '상품 검색/조회', 'PG_MKT_LIST', '상품 검색 및 조회', 'market/product/list', 'WEB'),
('주문/배차', '주문내역', 'PG_ORDER_LIST', '주문 내역 관리', 'order/history/list', 'WEB'),
('시스템관리', '권한설정관리', 'PG_SYS_AUTH', '권한 설정 매트릭스', 'admin/system/auth', 'WEB'),
(NULL, NULL, 'API_DASH_STATS', '대시보드 통계 API', '/api/v1/dash/stats', 'API'),
(NULL, NULL, 'API_MKT_SEARCH', '마켓 상품 검색 API', '/api/v1/market/search', 'API'),
(NULL, NULL, 'API_AUTH_SYNC', 'RBAC 권한 동기화 API', '/api/v1/internal/rbac/sync', 'API'),
(NULL, NULL, 'API_ORDER_DETAIL', '주문 상세 정보 조회 API', '/api/v1/order/detail', 'API');

-- 2) 역할 생성
INSERT INTO roles (id, name, description) VALUES 
('00000000-0000-0000-0000-000000000001', 'UNVERIFIED', '미인증 회원 (조회만 가능)'),
('00000000-0000-0000-0000-000000000002', 'USER', '인증 회원 (구매/판매 가능)'),
('00000000-0000-0000-0000-000000000003', 'ADMIN', '운영자 (모든 프로그램 접근)');

-- 3) 역할별 프로그램 매핑
-- 미인증 회원: 상품 검색 API만
INSERT INTO role_programs (role_id, program_id) 
SELECT r.id, p.id FROM roles r, programs p WHERE r.name = 'UNVERIFIED' AND p.program_code IN ('API_MKT_SEARCH');

-- 인증 회원: 상품 검색/조회 화면 및 API, 주문 화면 및 API
INSERT INTO role_programs (role_id, program_id) 
SELECT r.id, p.id FROM roles r, programs p WHERE r.name = 'USER' AND p.program_code IN ('PG_MKT_LIST', 'API_MKT_SEARCH', 'PG_ORDER_LIST', 'API_ORDER_DETAIL');

-- 운영자: 전체
INSERT INTO role_programs (role_id, program_id) 
SELECT r.id, p.id FROM roles r, programs p WHERE r.name = 'ADMIN';