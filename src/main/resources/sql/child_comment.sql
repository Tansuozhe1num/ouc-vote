CREATE TABLE `child_comment_info` (
    `c_id` INT NOT NULL AUTO_INCREMENT COMMENT '子评论ID',
    `snap_id` INT NOT NULL COMMENT '关联的snap ID',
    `parent_id` INT NOT NULL COMMENT '父级评论ID（comment_info.comment_id）',
    `text` VARCHAR(1000) NOT NULL COMMENT '评论内容',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0正常 1删除',
    `user_id` VARCHAR(64) NOT NULL COMMENT '评论用户ID',

    PRIMARY KEY (`c_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_snap_id` (`snap_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_status` (`parent_id`, `status`),
    KEY `idx_snap_status` (`snap_id`, `status`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
    COMMENT='子评论信息表';
