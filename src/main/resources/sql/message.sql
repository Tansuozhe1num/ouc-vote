CREATE TABLE IF NOT EXISTS message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sender_id BIGINT NOT NULL, receiver_id BIGINT NOT NULL, snap_id INT DEFAULT NULL,
    content VARCHAR(1000) NOT NULL, status TINYINT NOT NULL DEFAULT 0, ctime INT NOT NULL,
    PRIMARY KEY (id), KEY idx_message_sender (sender_id,receiver_id,id),
    KEY idx_message_receiver (receiver_id,sender_id,id), KEY idx_message_snap (snap_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内私信';
