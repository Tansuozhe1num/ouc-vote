CREATE TABLE IF NOT EXISTS `user` (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '雪花算法用户ID，业务关联使用此字段',
  login_name VARCHAR(64) NOT NULL,
  password_hash VARCHAR(100) DEFAULT NULL,
  name VARCHAR(64) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1封禁 2注销',
  description VARCHAR(500) DEFAULT NULL,
  sex TINYINT DEFAULT NULL COMMENT '0男 1女 NULL未设置',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台用户';

CREATE TABLE IF NOT EXISTS user_follow (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '被关注用户',
  follower_id BIGINT NOT NULL COMMENT '关注者',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0关注中 1已取消',
  ctime INT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_follow_pair (user_id,follower_id),
  KEY idx_following (follower_id,status,ctime),
  KEY idx_followers (user_id,status,ctime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注关系';

CREATE TABLE IF NOT EXISTS snap (
  snap_id INT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  snap_image VARCHAR(512) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0寻找中 1停止/删除 2仅自己可见',
  score INT NOT NULL DEFAULT 100 COMMENT '热度分',
  title VARCHAR(100) NOT NULL DEFAULT '',
  `desc` VARCHAR(500) NOT NULL,
  type INT NOT NULL COMMENT '0学习 1旅游 2恋爱 3其他 4游戏',
  ctime BIGINT NOT NULL,
  PRIMARY KEY (snap_id),
  KEY idx_snap_feed (status,type,snap_id),
  KEY idx_snap_user (user_id,status,snap_id),
  KEY idx_snap_random (status,snap_id),
  KEY idx_snap_score_decay (ctime,snap_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='找搭子信息';

CREATE TABLE IF NOT EXISTS message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  sender_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  snap_id INT DEFAULT NULL,
  content VARCHAR(1000) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1删除',
  ctime INT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_message_sender (sender_id,status,receiver_id,id),
  KEY idx_message_receiver (receiver_id,status,sender_id,id),
  KEY idx_message_snap (snap_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内私信';

CREATE TABLE IF NOT EXISTS comment_info (
  comment_id INT NOT NULL AUTO_INCREMENT,
  snap_id INT NOT NULL,
  `text` VARCHAR(1000) NOT NULL,
  ctime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status TINYINT NOT NULL DEFAULT 0,
  user_id VARCHAR(64) NOT NULL,
  PRIMARY KEY (comment_id),
  KEY idx_comment_snap_status (snap_id,status,comment_id),
  KEY idx_comment_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搭子动态评论';

CREATE TABLE IF NOT EXISTS child_comment_info (
  c_id INT NOT NULL AUTO_INCREMENT,
  snap_id INT NOT NULL,
  parent_id INT NOT NULL,
  `text` VARCHAR(1000) NOT NULL,
  ctime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status TINYINT NOT NULL DEFAULT 0,
  user_id VARCHAR(64) NOT NULL,
  PRIMARY KEY (c_id),
  KEY idx_child_parent_status (parent_id,status,c_id),
  KEY idx_child_snap_status (snap_id,status,c_id),
  KEY idx_child_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论回复';
