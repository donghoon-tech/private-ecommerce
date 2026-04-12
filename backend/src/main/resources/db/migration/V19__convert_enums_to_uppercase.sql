-- Refactor Enum values to Uppercase for consistency with Java naming conventions
-- Handle CHECK constraints along with the case conversion

-- 1. Business Profiles
ALTER TABLE business_profiles DROP CONSTRAINT IF EXISTS business_profiles_status_check;
UPDATE business_profiles SET status = UPPER(status);
ALTER TABLE business_profiles ADD CONSTRAINT business_profiles_status_check CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'));

-- 2. Products
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_status_check;
UPDATE products SET status = UPPER(status);
ALTER TABLE products ADD CONSTRAINT products_status_check CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'SELLING', 'SOLD_OUT'));

-- 3. Orders
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_order_type_check;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_truck_type_check;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_payment_status_check;

UPDATE orders SET 
    order_type = UPPER(order_type), 
    truck_type = UPPER(truck_type), 
    status = UPPER(status), 
    payment_status = UPPER(payment_status);

ALTER TABLE orders ADD CONSTRAINT orders_order_type_check CHECK (order_type IN ('PLATFORM', 'PHONE'));
ALTER TABLE orders ADD CONSTRAINT orders_truck_type_check CHECK (truck_type IN ('CARGO', 'WINGBODY'));
ALTER TABLE orders ADD CONSTRAINT orders_status_check CHECK (status IN ('PENDING', 'SHIPPING', 'DELIVERED', 'COMPLETED'));
ALTER TABLE orders ADD CONSTRAINT orders_payment_status_check CHECK (payment_status IN ('PENDING', 'CONFIRMED', 'SETTLED'));

-- 4. Notifications
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_channel_check;
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_status_check;

UPDATE notifications SET 
    channel = UPPER(channel), 
    status = UPPER(status);

ALTER TABLE notifications ADD CONSTRAINT notifications_channel_check CHECK (channel IN ('SMS', 'EMAIL'));
ALTER TABLE notifications ADD CONSTRAINT notifications_status_check CHECK (status IN ('PENDING', 'SENT', 'FAILED'));

-- 5. Order Images
ALTER TABLE order_images DROP CONSTRAINT IF EXISTS order_images_image_type_check;
UPDATE order_images SET image_type = UPPER(image_type);
ALTER TABLE order_images ADD CONSTRAINT order_images_image_type_check CHECK (image_type IN ('LOADING', 'DELIVERY'));
