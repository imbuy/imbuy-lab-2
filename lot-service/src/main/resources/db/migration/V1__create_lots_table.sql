-- Создание таблицы lots
CREATE TABLE lots (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_price DECIMAL(19, 2) NOT NULL,
    current_price DECIMAL(19, 2) NOT NULL,
    bid_step DECIMAL(19, 2) NOT NULL,
    owner_id BIGINT NOT NULL,
    category_id BIGINT,
    winner_id BIGINT,
    status VARCHAR(50) NOT NULL,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Создание индексов для оптимизации запросов
CREATE INDEX idx_lots_owner_id ON lots(owner_id);
CREATE INDEX idx_lots_category_id ON lots(category_id);
CREATE INDEX idx_lots_status ON lots(status);

