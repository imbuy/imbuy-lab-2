-- Создание таблицы categories
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_id BIGINT
);

-- Создание внешнего ключа для иерархии категорий
ALTER TABLE categories
    ADD CONSTRAINT fk_parent_category
    FOREIGN KEY (parent_id)
    REFERENCES categories(id)
    ON DELETE CASCADE;

-- Создание индекса для parent_id для оптимизации запросов
CREATE INDEX idx_categories_parent_id ON categories(parent_id);

