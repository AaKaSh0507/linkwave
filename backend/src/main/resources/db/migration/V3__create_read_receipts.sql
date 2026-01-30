CREATE TABLE read_receipts (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(36) NOT NULL,
    room_id VARCHAR(36) NOT NULL,
    reader_phone_number VARCHAR(20) NOT NULL,
    read_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_read_receipt UNIQUE (message_id, reader_phone_number),
    CONSTRAINT fk_read_receipt_room FOREIGN KEY (room_id) 
        REFERENCES chat_rooms(id) ON DELETE CASCADE
);

CREATE INDEX idx_read_receipts_message ON read_receipts(message_id);
CREATE INDEX idx_read_receipts_room_reader ON read_receipts(room_id, reader_phone_number);
CREATE INDEX idx_read_receipts_room ON read_receipts(room_id);
