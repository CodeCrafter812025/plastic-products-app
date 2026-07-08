# **PostgreSQL DDL فایل‌محور نهایی**

## **اپلیکیشن فروش محصولات پلاستیکی**

این بسته شامل سه فایل است:

* `schema.sql`  
* `seed.sql`  
* `down.sql`

ترتیب اجرا:

1. `schema.sql`  
2. `seed.sql`  
3. در صورت نیاز برای حذف کامل: `down.sql`

---

# **1\) `schema.sql`**

\-- \=========================================================  
\-- schema.sql  
\-- PostgreSQL DDL for Plastic Products Sales Application  
\-- \=========================================================

BEGIN;

\-- \---------------------------------------------------------  
\-- Extension  
\-- \---------------------------------------------------------  
CREATE EXTENSION IF NOT EXISTS pg\_trgm;

\-- \---------------------------------------------------------  
\-- ENUM Types  
\-- \---------------------------------------------------------  
CREATE TYPE user\_role\_enum AS ENUM ('admin', 'buyer', 'visitor');  
CREATE TYPE order\_status\_enum AS ENUM ('pending', 'assigned', 'loading', 'delivered', 'cancelled');  
CREATE TYPE product\_quality\_enum AS ENUM ('اولیه', 'بازیافتی');  
CREATE TYPE otp\_purpose\_enum AS ENUM ('register', 'login', 'change\_phone');  
CREATE TYPE deletion\_status\_enum AS ENUM ('pending', 'approved', 'rejected');  
CREATE TYPE stock\_reason\_enum AS ENUM ('initial', 'sale', 'restock', 'adjustment');  
CREATE TYPE notification\_type\_enum AS ENUM ('sms', 'push');

\-- \---------------------------------------------------------  
\-- Base Tables  
\-- \---------------------------------------------------------

CREATE TABLE users (  
    id BIGSERIAL PRIMARY KEY,  
    phone VARCHAR(11) NOT NULL UNIQUE,  
    full\_name VARCHAR(100) NOT NULL,  
    address TEXT,  
    role user\_role\_enum NOT NULL,  
    is\_active BOOLEAN NOT NULL DEFAULT TRUE,  
    created\_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),  
    updated\_at TIMESTAMP WITH TIME ZONE  
);

CREATE TABLE system\_settings (  
    setting\_key VARCHAR(100) PRIMARY KEY,  
    setting\_value TEXT NOT NULL,  
    description TEXT,  
    updated\_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()  
);

\-- \---------------------------------------------------------  
\-- Core Business Tables  
\-- \---------------------------------------------------------

CREATE TABLE products (  
    id BIGSERIAL PRIMARY KEY,  
    title VARCHAR(200) NOT NULL,  
    price DECIMAL(15,2) NOT NULL,  
    weight DECIMAL(10,2) NOT NULL,  
    color VARCHAR(50),  
    quality product\_quality\_enum NOT NULL,  
    description TEXT,  
    image\_urls JSONB NOT NULL DEFAULT '\[\]'::jsonb,  
    stock INTEGER NOT NULL DEFAULT 0,  
    is\_active BOOLEAN NOT NULL DEFAULT TRUE,  
    created\_by BIGINT NOT NULL,  
    created\_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),  
    updated\_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk\_products\_price\_nonnegative CHECK (price \>= 0),  
    CONSTRAINT chk\_products\_stock\_nonnegative CHECK (stock \>= 0),  
    CONSTRAINT fk\_products\_created\_by  
        FOREIGN KEY (created\_by) REFERENCES users(id) ON DELETE RESTRICT  
);

CREATE TABLE otp\_codes (  
    id BIGSERIAL PRIMARY KEY,  
    phone VARCHAR(11) NOT NULL,  
    code VARCHAR(5) NOT NULL,  
    purpose otp\_purpose\_enum NOT NULL,  
    expires\_at TIMESTAMP WITH TIME ZONE NOT NULL,  
    is\_used BOOLEAN NOT NULL DEFAULT FALSE,  
    attempt\_count INTEGER NOT NULL DEFAULT 0,  
    locked\_until TIMESTAMP WITH TIME ZONE,  
    created\_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT chk\_otp\_attempt\_count\_nonnegative CHECK (attempt\_count \>= 0\)  
);

CREATE TABLE account\_deletion\_requests (  
    id BIGSERIAL PRIMARY KEY,  
    user\_id BIGINT NOT NULL,  
    status deletion\_status\_enum NOT NULL DEFAULT 'pending',  
    requested\_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),  
    reviewed\_by BIGINT,  
    reviewed\_at TIMESTAMP WITH TIME ZONE,  
    admin\_note TEXT,

    CONSTRAINT fk\_account\_deletion\_requests\_user  
        FOREIGN KEY (user\_id) REFERENCES users(id) ON DELETE CASCADE,  
    CONSTRAINT fk\_account\_deletion\_requests\_reviewer  
        FOREIGN KEY (reviewed\_by) REFERENCES users(id) ON DELETE SET NULL  
);

CREATE TABLE cart\_items (  
    id BIGSERIAL PRIMARY KEY,  
    user\_id BIGINT NOT NULL,  
    product\_id BIGINT NOT NULL,  
    quantity DECIMAL(10,2) NOT NULL,  
    added\_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),  
    updated\_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk\_cart\_items\_quantity\_positive CHECK (quantity \> 0),  
    CONSTRAINT fk\_cart\_items\_user  
        FOREIGN KEY (user\_id) REFERENCES users(id) ON DELETE CASCADE,  
    CONSTRAINT fk\_cart\_items\_product  
        FOREIGN KEY (product\_id) REFERENCES products(id) ON DELETE CASCADE,  
    CONSTRAINT uq\_cart\_items\_user\_product UNIQUE (user\_id, product\_id)  
);

CREATE TABLE orders (  
    id BIGSERIAL PRIMARY KEY,  
    buyer\_id BIGINT NOT NULL,  
    visitor\_id BIGINT,  
    total\_price DECIMAL(15,2) NOT NULL DEFAULT 0,  
    status order\_status\_enum NOT NULL DEFAULT 'pending',  
    created\_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),  
    updated\_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk\_orders\_total\_price\_nonnegative CHECK (total\_price \>= 0),  
    CONSTRAINT fk\_orders\_buyer  
        FOREIGN KEY (buyer\_id) REFERENCES users(id) ON DELETE RESTRICT,  
    CONSTRAINT fk\_orders\_visitor  
        FOREIGN KEY (visitor\_id) REFERENCES users(id) ON DELETE SET NULL  
);

CREATE TABLE order\_items (  
    id BIGSERIAL PRIMARY KEY,  
    order\_id BIGINT NOT NULL,  
    product\_id BIGINT NOT NULL,  
    quantity DECIMAL(10,2) NOT NULL,  
    unit\_price DECIMAL(15,2) NOT NULL,  
    total\_price DECIMAL(15,2) GENERATED ALWAYS AS (quantity \* unit\_price) STORED,

    CONSTRAINT chk\_order\_items\_quantity\_positive CHECK (quantity \> 0),  
    CONSTRAINT chk\_order\_items\_unit\_price\_nonnegative CHECK (unit\_price \>= 0),  
    CONSTRAINT fk\_order\_items\_order  
        FOREIGN KEY (order\_id) REFERENCES orders(id) ON DELETE CASCADE,  
    CONSTRAINT fk\_order\_items\_product  
        FOREIGN KEY (product\_id) REFERENCES products(id) ON DELETE RESTRICT  
);

CREATE TABLE order\_assignments (  
    id BIGSERIAL PRIMARY KEY,  
    order\_id BIGINT NOT NULL,  
    old\_visitor\_id BIGINT,  
    new\_visitor\_id BIGINT NOT NULL,  
    assigned\_by BIGINT NOT NULL,  
    reason TEXT,  
    assigned\_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk\_order\_assignments\_order  
        FOREIGN KEY (order\_id) REFERENCES orders(id) ON DELETE CASCADE,  
    CONSTRAINT fk\_order\_assignments\_old\_visitor  
        FOREIGN KEY (old\_visitor\_id) REFERENCES users(id) ON DELETE SET NULL,  
    CONSTRAINT fk\_order\_assignments\_new\_visitor  
        FOREIGN KEY (new\_visitor\_id) REFERENCES users(id) ON DELETE RESTRICT,  
    CONSTRAINT fk\_order\_assignments\_assigned\_by  
        FOREIGN KEY (assigned\_by) REFERENCES users(id) ON DELETE RESTRICT  
);

CREATE TABLE order\_status\_histories (  
    id BIGSERIAL PRIMARY KEY,  
    order\_id BIGINT NOT NULL,  
    old\_status order\_status\_enum,  
    new\_status order\_status\_enum NOT NULL,  
    changed\_by BIGINT NOT NULL,  
    note TEXT,  
    changed\_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk\_order\_status\_histories\_order  
        FOREIGN KEY (order\_id) REFERENCES orders(id) ON DELETE CASCADE,  
    CONSTRAINT fk\_order\_status\_histories\_changed\_by  
        FOREIGN KEY (changed\_by) REFERENCES users(id) ON DELETE RESTRICT  
);

CREATE TABLE price\_histories (  
    id BIGSERIAL PRIMARY KEY,  
    product\_id BIGINT NOT NULL,  
    old\_price DECIMAL(15,2),  
    new\_price DECIMAL(15,2) NOT NULL,  
    changed\_by BIGINT NOT NULL,  
    changed\_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT chk\_price\_histories\_new\_price\_nonnegative CHECK (new\_price \>= 0),  
    CONSTRAINT fk\_price\_histories\_product  
        FOREIGN KEY (product\_id) REFERENCES products(id) ON DELETE RESTRICT,  
    CONSTRAINT fk\_price\_histories\_changed\_by  
        FOREIGN KEY (changed\_by) REFERENCES users(id) ON DELETE RESTRICT  
);

CREATE TABLE stock\_histories (  
    id BIGSERIAL PRIMARY KEY,  
    product\_id BIGINT NOT NULL,  
    old\_stock INTEGER,  
    new\_stock INTEGER NOT NULL,  
    reason stock\_reason\_enum NOT NULL,  
    changed\_by BIGINT NOT NULL,  
    changed\_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT chk\_stock\_histories\_new\_stock\_nonnegative CHECK (new\_stock \>= 0),  
    CONSTRAINT fk\_stock\_histories\_product  
        FOREIGN KEY (product\_id) REFERENCES products(id) ON DELETE RESTRICT,  
    CONSTRAINT fk\_stock\_histories\_changed\_by  
        FOREIGN KEY (changed\_by) REFERENCES users(id) ON DELETE RESTRICT  
);

CREATE TABLE notifications (  
    id BIGSERIAL PRIMARY KEY,  
    user\_id BIGINT NOT NULL,  
    related\_type VARCHAR(30),  
    related\_id BIGINT,  
    type notification\_type\_enum NOT NULL,  
    message TEXT NOT NULL,  
    sent\_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),  
    is\_read BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk\_notifications\_user  
        FOREIGN KEY (user\_id) REFERENCES users(id) ON DELETE CASCADE  
);

\-- \---------------------------------------------------------  
\-- Helper Functions  
\-- \---------------------------------------------------------

CREATE OR REPLACE FUNCTION set\_updated\_at()  
RETURNS TRIGGER AS $$  
BEGIN  
    NEW.updated\_at \= NOW();  
    RETURN NEW;  
END;  
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION recalculate\_order\_total(p\_order\_id BIGINT)  
RETURNS VOID AS $$  
BEGIN  
    UPDATE orders  
    SET total\_price \= COALESCE(  
        (SELECT SUM(oi.total\_price)  
         FROM order\_items oi  
         WHERE oi.order\_id \= p\_order\_id),  
        0  
    ),  
    updated\_at \= NOW()  
    WHERE id \= p\_order\_id;  
END;  
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION refresh\_order\_total\_trigger()  
RETURNS TRIGGER AS $$  
BEGIN  
    IF TG\_OP \= 'INSERT' THEN  
        PERFORM recalculate\_order\_total(NEW.order\_id);  
        RETURN NEW;

    ELSIF TG\_OP \= 'DELETE' THEN  
        PERFORM recalculate\_order\_total(OLD.order\_id);  
        RETURN OLD;

    ELSIF TG\_OP \= 'UPDATE' THEN  
        PERFORM recalculate\_order\_total(OLD.order\_id);

        IF NEW.order\_id IS DISTINCT FROM OLD.order\_id THEN  
            PERFORM recalculate\_order\_total(NEW.order\_id);  
        END IF;

        RETURN NEW;  
    END IF;

    RETURN NULL;  
END;  
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION insert\_initial\_product\_history()  
RETURNS TRIGGER AS $$  
BEGIN  
    INSERT INTO price\_histories (product\_id, old\_price, new\_price, changed\_by, changed\_at)  
    VALUES (NEW.id, NULL, NEW.price, NEW.created\_by, NOW());

    INSERT INTO stock\_histories (product\_id, old\_stock, new\_stock, reason, changed\_by, changed\_at)  
    VALUES (NEW.id, NULL, NEW.stock, 'initial', NEW.created\_by, NOW());

    RETURN NEW;  
END;  
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION sync\_order\_visitor()  
RETURNS TRIGGER AS $$  
BEGIN  
    UPDATE orders  
    SET visitor\_id \= NEW.new\_visitor\_id,  
        status \= 'assigned',  
        updated\_at \= NOW()  
    WHERE id \= NEW.order\_id;

    RETURN NEW;  
END;  
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION validate\_order\_status\_transition()  
RETURNS TRIGGER AS $$  
BEGIN  
    IF NEW.status \= OLD.status THEN  
        RETURN NEW;  
    END IF;

    IF OLD.status \= 'pending' AND NEW.status IN ('assigned', 'cancelled') THEN  
        RETURN NEW;  
    ELSIF OLD.status \= 'assigned' AND NEW.status IN ('loading', 'cancelled') THEN  
        RETURN NEW;  
    ELSIF OLD.status \= 'loading' AND NEW.status IN ('delivered', 'cancelled') THEN  
        RETURN NEW;  
    ELSIF OLD.status IN ('delivered', 'cancelled') THEN  
        RAISE EXCEPTION 'Order status cannot change from % to %', OLD.status, NEW.status;  
    END IF;

    RAISE EXCEPTION 'Invalid order status transition from % to %', OLD.status, NEW.status;  
END;  
$$ LANGUAGE plpgsql;

\-- \---------------------------------------------------------  
\-- Triggers  
\-- \---------------------------------------------------------

CREATE TRIGGER trg\_users\_updated\_at  
BEFORE UPDATE ON users  
FOR EACH ROW EXECUTE FUNCTION set\_updated\_at();

CREATE TRIGGER trg\_products\_updated\_at  
BEFORE UPDATE ON products  
FOR EACH ROW EXECUTE FUNCTION set\_updated\_at();

CREATE TRIGGER trg\_cart\_items\_updated\_at  
BEFORE UPDATE ON cart\_items  
FOR EACH ROW EXECUTE FUNCTION set\_updated\_at();

CREATE TRIGGER trg\_orders\_updated\_at  
BEFORE UPDATE ON orders  
FOR EACH ROW EXECUTE FUNCTION set\_updated\_at();

CREATE TRIGGER trg\_system\_settings\_updated\_at  
BEFORE UPDATE ON system\_settings  
FOR EACH ROW EXECUTE FUNCTION set\_updated\_at();

CREATE TRIGGER trg\_order\_items\_refresh\_total\_after\_insert  
AFTER INSERT ON order\_items  
FOR EACH ROW EXECUTE FUNCTION refresh\_order\_total\_trigger();

CREATE TRIGGER trg\_order\_items\_refresh\_total\_after\_update  
AFTER UPDATE ON order\_items  
FOR EACH ROW EXECUTE FUNCTION refresh\_order\_total\_trigger();

CREATE TRIGGER trg\_order\_items\_refresh\_total\_after\_delete  
AFTER DELETE ON order\_items  
FOR EACH ROW EXECUTE FUNCTION refresh\_order\_total\_trigger();

CREATE TRIGGER trg\_products\_initial\_history\_after\_insert  
AFTER INSERT ON products  
FOR EACH ROW EXECUTE FUNCTION insert\_initial\_product\_history();

CREATE TRIGGER trg\_order\_assignments\_sync\_visitor\_after\_insert  
AFTER INSERT ON order\_assignments  
FOR EACH ROW EXECUTE FUNCTION sync\_order\_visitor();

CREATE TRIGGER trg\_orders\_validate\_status\_transition  
BEFORE UPDATE OF status ON orders  
FOR EACH ROW EXECUTE FUNCTION validate\_order\_status\_transition();

\-- \---------------------------------------------------------  
\-- Indexes  
\-- \---------------------------------------------------------

CREATE INDEX idx\_users\_phone ON users(phone);  
CREATE INDEX idx\_otp\_codes\_phone ON otp\_codes(phone);

CREATE INDEX idx\_products\_created\_by ON products(created\_by);  
CREATE INDEX idx\_products\_is\_active ON products(is\_active);  
CREATE INDEX idx\_products\_title\_trgm ON products USING GIN (title gin\_trgm\_ops);

CREATE INDEX idx\_cart\_items\_user\_id ON cart\_items(user\_id);  
CREATE INDEX idx\_cart\_items\_product\_id ON cart\_items(product\_id);

CREATE INDEX idx\_orders\_buyer\_id ON orders(buyer\_id);  
CREATE INDEX idx\_orders\_visitor\_id ON orders(visitor\_id);  
CREATE INDEX idx\_orders\_status ON orders(status);

CREATE INDEX idx\_order\_items\_order\_id ON order\_items(order\_id);  
CREATE INDEX idx\_order\_items\_product\_id ON order\_items(product\_id);

CREATE INDEX idx\_order\_assignments\_order\_id ON order\_assignments(order\_id);  
CREATE INDEX idx\_order\_assignments\_new\_visitor\_id ON order\_assignments(new\_visitor\_id);

CREATE INDEX idx\_order\_status\_histories\_order\_id ON order\_status\_histories(order\_id);

CREATE INDEX idx\_price\_histories\_product\_id ON price\_histories(product\_id);  
CREATE INDEX idx\_price\_histories\_changed\_by ON price\_histories(changed\_by);

CREATE INDEX idx\_stock\_histories\_product\_id ON stock\_histories(product\_id);  
CREATE INDEX idx\_stock\_histories\_changed\_by ON stock\_histories(changed\_by);

CREATE INDEX idx\_notifications\_user\_id ON notifications(user\_id);  
CREATE INDEX idx\_notifications\_related ON notifications(related\_type, related\_id);

CREATE INDEX idx\_account\_deletion\_requests\_user\_id ON account\_deletion\_requests(user\_id);  
CREATE INDEX idx\_account\_deletion\_requests\_status ON account\_deletion\_requests(status);

COMMIT;

---

# **2\) `seed.sql`**

\-- \=========================================================  
\-- seed.sql  
\-- Initial seed data  
\-- \=========================================================

BEGIN;

INSERT INTO system\_settings (setting\_key, setting\_value, description)  
VALUES (  
    'factory\_phone',  
    '09120000000',  
    'Main contact number of the factory shown in product details'  
)  
ON CONFLICT (setting\_key)  
DO UPDATE SET  
    setting\_value \= EXCLUDED.setting\_value,  
    description \= EXCLUDED.description,  
    updated\_at \= NOW();

COMMIT;

---

# **3\) `down.sql`**

\-- \=========================================================  
\-- down.sql  
\-- Rollback script  
\-- \=========================================================

BEGIN;

DROP TABLE IF EXISTS notifications;  
DROP TABLE IF EXISTS stock\_histories;  
DROP TABLE IF EXISTS price\_histories;  
DROP TABLE IF EXISTS order\_status\_histories;  
DROP TABLE IF EXISTS order\_assignments;  
DROP TABLE IF EXISTS order\_items;  
DROP TABLE IF EXISTS orders;  
DROP TABLE IF EXISTS cart\_items;  
DROP TABLE IF EXISTS account\_deletion\_requests;  
DROP TABLE IF EXISTS otp\_codes;  
DROP TABLE IF EXISTS products;  
DROP TABLE IF EXISTS system\_settings;  
DROP TABLE IF EXISTS users;

DROP FUNCTION IF EXISTS validate\_order\_status\_transition();  
DROP FUNCTION IF EXISTS sync\_order\_visitor();  
DROP FUNCTION IF EXISTS insert\_initial\_product\_history();  
DROP FUNCTION IF EXISTS refresh\_order\_total\_trigger();  
DROP FUNCTION IF EXISTS recalculate\_order\_total(BIGINT);  
DROP FUNCTION IF EXISTS set\_updated\_at();

DROP TYPE IF EXISTS notification\_type\_enum;  
DROP TYPE IF EXISTS stock\_reason\_enum;  
DROP TYPE IF EXISTS deletion\_status\_enum;  
DROP TYPE IF EXISTS otp\_purpose\_enum;  
DROP TYPE IF EXISTS product\_quality\_enum;  
DROP TYPE IF EXISTS order\_status\_enum;  
DROP TYPE IF EXISTS user\_role\_enum;

DROP EXTENSION IF EXISTS pg\_trgm;

COMMIT;

---

## **نکات اجرایی**

* `schema.sql` فقط یک‌بار هر جدول را تعریف می‌کند.  
* `image_urls` اگر خالی باشد، مقدار پیش‌فرض `[]` می‌گیرد.  
* `total_price` سفارش بعد از تغییر `order_items` دوباره محاسبه می‌شود.  
* کنترل مسیرهای مجاز وضعیت سفارش با trigger انجام می‌شود.  
* حذف `CartItem` بعد از ثبت سفارش همچنان باید در لایه برنامه انجام شود.  
* `factory_phone` از `system_settings` خوانده می‌شود.

---

## **جمع‌بندی**

این نسخه از نظر تکرار جدول‌ها پاک‌سازی شده و برای اجرای مستقیم در PostgreSQL آماده است.

