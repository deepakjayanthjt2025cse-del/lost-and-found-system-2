-- ============================================================
--  LOST AND FOUND SYSTEM  -  REPAIR / COMPLETE THE V2 UPGRADE
--  Safe to run any number of times: it only adds what is MISSING
--  and never deletes your data.
--
--  HOW TO RUN IN WORKBENCH:
--    1. File > Open SQL Script > choose this file
--    2. Click NOTHING in the editor (no selected text)
--    3. Press the lightning bolt icon (or Ctrl+Shift+Enter)
--    4. Read the result grids at the bottom (see the end of the file)
-- ============================================================

USE lost_and_found_db;

-- password column must be wide enough for a hash
ALTER TABLE admins MODIFY COLUMN password VARCHAR(255) NOT NULL;

-- admins.role
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = 'lost_and_found_db' AND TABLE_NAME = 'admins' AND COLUMN_NAME = 'role') = 0,
    'ALTER TABLE admins ADD COLUMN role ENUM(''SUPER_ADMIN'',''STAFF'') NOT NULL DEFAULT ''STAFF'' AFTER password',
    'SELECT ''admins.role already exists'' AS info'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- admins.failed_attempts
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = 'lost_and_found_db' AND TABLE_NAME = 'admins' AND COLUMN_NAME = 'failed_attempts') = 0,
    'ALTER TABLE admins ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0 AFTER role',
    'SELECT ''admins.failed_attempts already exists'' AS info'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- admins.locked_until
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = 'lost_and_found_db' AND TABLE_NAME = 'admins' AND COLUMN_NAME = 'locked_until') = 0,
    'ALTER TABLE admins ADD COLUMN locked_until DATETIME NULL AFTER failed_attempts',
    'SELECT ''admins.locked_until already exists'' AS info'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- lost_items.colour
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = 'lost_and_found_db' AND TABLE_NAME = 'lost_items' AND COLUMN_NAME = 'colour') = 0,
    'ALTER TABLE lost_items ADD COLUMN colour VARCHAR(30) NOT NULL DEFAULT ''Not specified'' AFTER lost_date',
    'SELECT ''lost_items.colour already exists'' AS info'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- lost_items.specifications
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = 'lost_and_found_db' AND TABLE_NAME = 'lost_items' AND COLUMN_NAME = 'specifications') = 0,
    'ALTER TABLE lost_items ADD COLUMN specifications VARCHAR(255) NULL AFTER colour',
    'SELECT ''lost_items.specifications already exists'' AS info'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- found_items.colour
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = 'lost_and_found_db' AND TABLE_NAME = 'found_items' AND COLUMN_NAME = 'colour') = 0,
    'ALTER TABLE found_items ADD COLUMN colour VARCHAR(30) NOT NULL DEFAULT ''Not specified'' AFTER found_date',
    'SELECT ''found_items.colour already exists'' AS info'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- found_items.specifications
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = 'lost_and_found_db' AND TABLE_NAME = 'found_items' AND COLUMN_NAME = 'specifications') = 0,
    'ALTER TABLE found_items ADD COLUMN specifications VARCHAR(255) NULL AFTER colour',
    'SELECT ''found_items.specifications already exists'' AS info'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- audit log table
CREATE TABLE IF NOT EXISTS admin_audit_log (
    log_id      INT          NOT NULL AUTO_INCREMENT,
    admin_id    INT          NULL,
    username    VARCHAR(50)  NOT NULL,
    action_type VARCHAR(30)  NOT NULL,
    details     VARCHAR(255) NULL,
    log_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    CONSTRAINT fk_audit_admin FOREIGN KEY (admin_id)
        REFERENCES admins (admin_id) ON UPDATE CASCADE ON DELETE SET NULL,
    INDEX idx_audit_time (log_time)
) ENGINE=InnoDB;

-- the original account becomes the super admin; add the demo staff account
UPDATE admins SET role = 'SUPER_ADMIN' WHERE username = 'admin';
INSERT IGNORE INTO admins (username, password, role) VALUES ('staff1', 'staff123', 'STAFF');

-- ============================================================
--  RESULT CHECK  (you should see these grids at the bottom)
--   1st: admin = SUPER_ADMIN, staff1 = STAFF
--   2nd: 9 rows -> every column below must be listed
-- ============================================================
SELECT admin_id, username, role, failed_attempts, locked_until FROM admins;

SELECT TABLE_NAME, COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'lost_and_found_db'
  AND ((TABLE_NAME = 'admins'      AND COLUMN_NAME IN ('role','failed_attempts','locked_until'))
    OR (TABLE_NAME = 'lost_items'  AND COLUMN_NAME IN ('colour','specifications'))
    OR (TABLE_NAME = 'found_items' AND COLUMN_NAME IN ('colour','specifications')))
ORDER BY TABLE_NAME, COLUMN_NAME;
