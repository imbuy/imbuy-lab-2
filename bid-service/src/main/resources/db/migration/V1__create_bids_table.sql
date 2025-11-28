-- Создание таблицы bids
CREATE TABLE bids (
    id BIGSERIAL PRIMARY KEY,
    lot_id BIGINT NOT NULL,
    bidder_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Создание индексов для оптимизации запросов
CREATE INDEX idx_bids_lot_id ON bids(lot_id);
CREATE INDEX idx_bids_bidder_id ON bids(bidder_id);
CREATE INDEX idx_bids_created_at ON bids(created_at);

