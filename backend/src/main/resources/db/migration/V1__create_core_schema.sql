CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(30),
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
);

CREATE TABLE resorts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    location VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_resorts_name UNIQUE (name),
    CONSTRAINT chk_resorts_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    resort_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(30) NOT NULL,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL,
    image_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_resort FOREIGN KEY (resort_id) REFERENCES resorts (id),
    CONSTRAINT uq_products_resort_name UNIQUE (resort_id, name),
    CONSTRAINT chk_products_category CHECK (
        category IN ('RESORT_ACCESS', 'LIFT_TICKET', 'LESSON', 'RENTAL')
    ),
    CONSTRAINT chk_products_price CHECK (price >= 0)
);

CREATE TABLE lesson_sessions (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    capacity INTEGER NOT NULL,
    booked_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_lesson_sessions_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uq_lesson_sessions_slot UNIQUE (product_id, session_date, start_time),
    CONSTRAINT chk_lesson_sessions_times CHECK (end_time > start_time),
    CONSTRAINT chk_lesson_sessions_capacity CHECK (capacity > 0),
    CONSTRAINT chk_lesson_sessions_booked_count CHECK (
        booked_count >= 0 AND booked_count <= capacity
    ),
    CONSTRAINT chk_lesson_sessions_status CHECK (status IN ('ACTIVE', 'CANCELLED'))
);

CREATE TABLE carts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    session_token VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_carts_session_token UNIQUE (session_token),
    CONSTRAINT chk_carts_status CHECK (status IN ('ACTIVE', 'CHECKED_OUT', 'ABANDONED')),
    CONSTRAINT chk_carts_owner CHECK (user_id IS NOT NULL OR session_token IS NOT NULL)
);

CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    lesson_session_id BIGINT,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    booking_date DATE,
    vehicle_registration VARCHAR(30),
    vehicle_type VARCHAR(50),
    entry_date DATE,
    exit_date DATE,
    rental_start_date DATE,
    rental_end_date DATE,
    rental_size VARCHAR(50),
    rental_boot_size VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_cart_items_lesson_session FOREIGN KEY (lesson_session_id) REFERENCES lesson_sessions (id),
    CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_cart_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_cart_items_entry_dates CHECK (
        exit_date IS NULL OR (entry_date IS NOT NULL AND exit_date >= entry_date)
    ),
    CONSTRAINT chk_cart_items_rental_dates CHECK (
        rental_end_date IS NULL
        OR (rental_start_date IS NOT NULL AND rental_end_date >= rental_start_date)
    )
);

CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    booking_number VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    customer_first_name VARCHAR(100) NOT NULL,
    customer_last_name VARCHAR(100) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(30),
    status VARCHAR(20) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'AUD',
    total_amount NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_bookings_booking_number UNIQUE (booking_number),
    CONSTRAINT chk_bookings_status CHECK (
        status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED')
    ),
    CONSTRAINT chk_bookings_currency CHECK (currency = UPPER(currency)),
    CONSTRAINT chk_bookings_total_amount CHECK (total_amount >= 0)
);

CREATE TABLE booking_items (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    lesson_session_id BIGINT,
    product_name VARCHAR(150) NOT NULL,
    category VARCHAR(30) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    subtotal NUMERIC(10, 2) NOT NULL,
    booking_date DATE,
    vehicle_registration VARCHAR(30),
    vehicle_type VARCHAR(50),
    entry_date DATE,
    exit_date DATE,
    rental_start_date DATE,
    rental_end_date DATE,
    rental_size VARCHAR(50),
    rental_boot_size VARCHAR(30),
    CONSTRAINT fk_booking_items_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_booking_items_lesson_session FOREIGN KEY (lesson_session_id) REFERENCES lesson_sessions (id),
    CONSTRAINT chk_booking_items_category CHECK (
        category IN ('RESORT_ACCESS', 'LIFT_TICKET', 'LESSON', 'RENTAL')
    ),
    CONSTRAINT chk_booking_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_booking_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_booking_items_subtotal CHECK (subtotal >= 0),
    CONSTRAINT chk_booking_items_entry_dates CHECK (
        exit_date IS NULL OR (entry_date IS NOT NULL AND exit_date >= entry_date)
    ),
    CONSTRAINT chk_booking_items_rental_dates CHECK (
        rental_end_date IS NULL
        OR (rental_start_date IS NOT NULL AND rental_end_date >= rental_start_date)
    )
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    stripe_payment_id VARCHAR(255),
    amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'AUD',
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(50),
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT uq_payments_stripe_payment_id UNIQUE (stripe_payment_id),
    CONSTRAINT chk_payments_amount CHECK (amount >= 0),
    CONSTRAINT chk_payments_currency CHECK (currency = UPPER(currency)),
    CONSTRAINT chk_payments_status CHECK (
        status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'REFUNDED')
    )
);

CREATE INDEX idx_products_resort_category_active
    ON products (resort_id, category, is_active);
CREATE INDEX idx_lesson_sessions_product_date_status
    ON lesson_sessions (product_id, session_date, status);
CREATE INDEX idx_carts_user_status ON carts (user_id, status);
CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);
CREATE INDEX idx_bookings_user_created_at ON bookings (user_id, created_at DESC);
CREATE INDEX idx_bookings_status ON bookings (status);
CREATE INDEX idx_booking_items_booking ON booking_items (booking_id);
CREATE INDEX idx_booking_items_category ON booking_items (category);
CREATE INDEX idx_payments_booking_created_at ON payments (booking_id, created_at DESC);
CREATE INDEX idx_payments_status ON payments (status);
