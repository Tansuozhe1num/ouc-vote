CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    login_name VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) DEFAULT NULL,
    name VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    description VARCHAR(500) DEFAULT NULL,
    sex TINYINT DEFAULT NULL,
    ctime INT NOT NULL,
    major VARCHAR(128) DEFAULT NULL,
    token VARCHAR(512) DEFAULT NULL,
    refreshtoken VARCHAR(128) DEFAULT NULL,
    wechat_openid VARCHAR(64) DEFAULT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_user_user_id (user_id), UNIQUE KEY uk_user_login_name (login_name),
    UNIQUE KEY uk_user_wechat_openid (wechat_openid), KEY idx_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台用户';
