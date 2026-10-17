ALTER TABLE snap
    ADD COLUMN score INT NOT NULL DEFAULT 100 COMMENT '热度分',
    ADD KEY idx_snap_score_decay (ctime, snap_id);

UPDATE snap s
LEFT JOIN (
    SELECT snap_id, COUNT(*) AS active_comments
    FROM comment_info
    WHERE status = 0
    GROUP BY snap_id
) c ON c.snap_id = s.snap_id
LEFT JOIN (
    SELECT snap_id, COUNT(*) AS active_replies
    FROM child_comment_info
    WHERE status = 0
    GROUP BY snap_id
) r ON r.snap_id = s.snap_id
SET s.score = 100
    + COALESCE(c.active_comments, 0) * 80
    + COALESCE(r.active_replies, 0) * 40;
