CREATE TABLE IF NOT EXISTS user_follow (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '被关注用户', follower_id BIGINT NOT NULL COMMENT '关注者',
    status TINYINT NOT NULL DEFAULT 0, ctime INT NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_follow_pair (user_id,follower_id),
    KEY idx_following (follower_id,status,ctime), KEY idx_followers (user_id,status,ctime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注关系';
