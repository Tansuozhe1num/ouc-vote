-- Campus Match migration for existing vote database.
-- Run during a maintenance window. The existing snap schema is replaced.

CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT 'Snowflake business user id',
    login_name VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) DEFAULT NULL,
    name VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0 normal, 1 banned, 2 closed',
    description VARCHAR(500) DEFAULT NULL,
    sex TINYINT NOT NULL DEFAULT 0 COMMENT '0 male, 1 female',
    ctime INT NOT NULL,
    major VARCHAR(128) DEFAULT NULL,
    token VARCHAR(512) DEFAULT NULL,
    refreshtoken VARCHAR(128) DEFAULT NULL,
    wechat_openid VARCHAR(64) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_user_id (user_id),
    UNIQUE KEY uk_user_login_name (login_name),
    UNIQUE KEY uk_user_wechat_openid (wechat_openid),
    KEY idx_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='platform users';

CREATE TABLE IF NOT EXISTS user_follow (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT 'user being followed',
    follower_id BIGINT NOT NULL COMMENT 'user who follows',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0 following, 1 unfollowed',
    ctime INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_follow_pair (user_id, follower_id),
    KEY idx_following (follower_id, status, ctime),
    KEY idx_followers (user_id, status, ctime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user follow relationships';

CREATE TABLE IF NOT EXISTS message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    snap_id INT DEFAULT NULL,
    content VARCHAR(1000) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0 normal, 1 deleted',
    ctime INT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_message_sender (sender_id, receiver_id, id),
    KEY idx_message_receiver (receiver_id, sender_id, id),
    KEY idx_message_snap (snap_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='direct messages';

ALTER TABLE `user` MODIFY sex TINYINT DEFAULT NULL COMMENT '0 male, 1 female, NULL unset';

-- Preserve old snap rows before replacement. Legacy rows have no user accounts;
-- they cannot be mapped to the new user_id model automatically.
RENAME TABLE snap TO snap_legacy_20261013;
CREATE TABLE snap (
    snap_id INT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    snap_image VARCHAR(512) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0 seeking, 1 stopped/deleted, 2 owner only',
    `desc` VARCHAR(500) NOT NULL,
    type INT NOT NULL COMMENT '0 study, 1 travel, 2 romance, 3 other',
    ctime BIGINT NOT NULL,
    PRIMARY KEY (snap_id),
    KEY idx_snap_feed (status, type, snap_id),
    KEY idx_snap_user (user_id, status, snap_id),
    KEY idx_snap_random (status, snap_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='campus partner posts';

-- Optional legacy data import. The fixed Snowflake id is reserved for migrated rows.
INSERT INTO `user` (user_id, login_name, name, status, description, sex, ctime, major)
VALUES (900000000000000001, 'legacy_snap_owner', '历史校园墙', 0, '从旧版校园墙迁移的动态', 0, UNIX_TIMESTAMP(), '校园动态')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

INSERT INTO snap (snap_id, user_id, snap_image, status, `desc`, type, ctime)
SELECT snap_id,
       900000000000000001,
       snap_image,
       CASE WHEN status IN (0, 1, 2) THEN status ELSE 1 END,
       COALESCE(NULLIF(TRIM(`desc`), ''), '历史校园墙动态'),
       3,
       COALESCE(ctime, UNIX_TIMESTAMP() * 1000)
FROM snap_legacy_20261013;
