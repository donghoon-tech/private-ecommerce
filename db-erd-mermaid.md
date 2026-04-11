erDiagram
    %% 1. RBAC (Roles & Permissions)
    roles ||--o{ role_menu_actions : "assigned_to"
    menus ||--o{ role_menu_actions : "actions_of"
    menus ||--o{ menus : "parent_id (self)"
    roles ||--o{ users : "defines_role"

    %% 2. User & Profiles
    users ||--o{ business_profiles : "owns"
    users ||--o{ business_profiles : "approves"
    users ||--o{ delivery_addresses : "manages"

    %% 3. Products & Categories
    categories ||--o{ categories : "parent_id (self)"
    categories ||--o{ products : "classifies"
    users ||--o{ products : "sells"
    users ||--o{ products : "approves"
    products ||--o{ product_images : "has"

    %% 4. Cart & Orders
    users ||--o{ cart_items : "adds"
    users ||--o{ cart_items : "is_seller_of"
    products ||--o{ cart_items : "referenced"
    
    users ||--o{ orders : "buys"
    users ||--o{ orders : "receives_order"
    orders ||--o{ order_items : "includes"
    products ||--o{ order_items : "referenced"
    orders ||--o{ order_images : "contains"
    users ||--o{ order_images : "uploaded_by"

    %% 5. Notifications
    users ||--o{ notifications : "receives"
    orders ||--o{ notifications : "triggers"

    roles {
        uuid id PK
        varchar name "Unique (e.g. ROLE_ADMIN, ROLE_USER)"
        text description
        timestamp created_at
    }

    menus {
        uuid id PK
        varchar menu_code "Unique (e.g. SYS_AUTH)"
        varchar name
        uuid parent_id FK
        int sort_order
        boolean is_visible
        timestamp created_at
    }

    role_menu_actions {
        uuid id PK
        uuid role_id FK
        uuid menu_id FK
        boolean can_read
        boolean can_create
        boolean can_update
        boolean can_delete
        boolean can_excel
        timestamp created_at
        timestamp updated_at
    }

    users {
        uuid id PK
        uuid role_id FK
        varchar username "Unique"
        text password_hash
        varchar name
        varchar representative_phone "Unique"
        varchar email
        varchar business_number
        boolean is_active
        timestamp phone_verified_at
        timestamp last_login_at
        int failed_login_count
        timestamp created_at
        timestamp updated_at
    }

    business_profiles {
        uuid id PK
        uuid user_id FK
        varchar business_name
        varchar business_number
        varchar representative_name
        text office_address
        text storage_address
        text br_image_url
        varchar status "pending/approved/rejected"
        text rejection_reason
        timestamp approved_at
        uuid approved_by FK
        boolean is_main
        timestamp created_at
        timestamp updated_at
    }

    delivery_addresses {
        uuid id PK
        uuid user_id FK
        varchar address_name
        text full_address
        text detail_address
        varchar recipient_name
        varchar recipient_phone
        boolean is_default
        timestamp created_at
        timestamp updated_at
    }

    categories {
        uuid id PK
        uuid parent_id FK
        varchar name
        int depth
        int display_order
        timestamp created_at
    }

    products {
        uuid id PK
        uuid seller_id FK
        uuid category_id FK
        varchar item_name
        varchar item_condition "신재/고재"
        decimal unit_price
        varchar sale_unit
        int stock_quantity
        decimal total_amount
        text loading_address
        varchar loading_address_display
        varchar status "pending/approved/rejected/selling/sold_out"
        text rejection_reason
        timestamp approved_at
        uuid approved_by FK
        boolean is_displayed
        timestamp created_at
        timestamp updated_at
    }

    product_images {
        uuid id PK
        uuid product_id FK
        text image_url
        int display_order
        timestamp created_at
    }

    cart_items {
        uuid id PK
        uuid user_id FK
        uuid product_id FK
        uuid seller_id FK
        int quantity
        timestamp created_at
        timestamp updated_at
    }

    orders {
        uuid id PK
        uuid buyer_id FK
        uuid seller_id FK
        varchar order_type "platform/phone"
        varchar truck_tonnage
        varchar truck_type "cargo/wingbody"
        text shipping_loading_address
        text shipping_unloading_address
        varchar recipient_name
        varchar recipient_phone
        decimal total_amount
        varchar status "pending/shipping/delivered/completed"
        varchar payment_status "pending/confirmed/settled"
        text order_memo
        text admin_memo
        timestamp delivery_started_at
        timestamp delivery_completed_at
        text carrier_info
        timestamp created_at
        timestamp updated_at
    }

    order_items {
        uuid id PK
        uuid order_id FK
        uuid product_id FK
        varchar product_name_snapshot
        varchar product_condition_snapshot
        decimal price_snapshot
        int quantity
        decimal subtotal
        timestamp created_at
    }

    order_images {
        uuid id PK
        uuid order_id FK
        uuid uploaded_by FK
        text image_url
        varchar image_type "loading/delivery"
        timestamp created_at
    }

    notifications {
        uuid id PK
        uuid user_id FK
        uuid order_id FK
        varchar type
        varchar channel "sms/email"
        text content
        timestamp sent_at
        varchar status "pending/sent/failed"
        timestamp created_at
    }