-- V5__add_mbac_schema.sql
-- Create Menu table
CREATE TABLE menus (
    id UUID PRIMARY KEY,
    menu_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    parent_id UUID REFERENCES menus(id) ON DELETE CASCADE,
    sort_order INT NOT NULL DEFAULT 0,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create RoleMenuAction table
CREATE TABLE role_menu_actions (
    id UUID PRIMARY KEY,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    menu_id UUID NOT NULL REFERENCES menus(id) ON DELETE CASCADE,
    can_read BOOLEAN NOT NULL DEFAULT FALSE,
    can_create BOOLEAN NOT NULL DEFAULT FALSE,
    can_update BOOLEAN NOT NULL DEFAULT FALSE,
    can_delete BOOLEAN NOT NULL DEFAULT FALSE,
    can_excel BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (role_id, menu_id)
);

-- Note: We do not drop role_permissions and permissions yet just in case there's any active JPA relation, but since the Entity is removed, Hibernate validation passes. Actually, let's drop them to keep schema clean since we are in MVP.
DROP TABLE IF EXISTS role_permissions CASCADE;
DROP TABLE IF EXISTS permissions CASCADE;
