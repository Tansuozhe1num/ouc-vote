ALTER TABLE snap
    ADD COLUMN title VARCHAR(100) NOT NULL DEFAULT '' AFTER user_id;

UPDATE snap
SET title = LEFT(TRIM(`desc`), 100)
WHERE title = '';
