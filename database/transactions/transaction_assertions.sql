USE anime_platform;

-- This script leaves no permanent transaction-demo rows.
DELETE FROM operation_log
WHERE action_type = 'RAW_TRANSACTION_COMMIT'
  AND description = 'explicit transaction commit demo';

START TRANSACTION;
INSERT INTO operation_log (
    operator_user_id, action_type, target_type, description, ip_address
) VALUES (
    1, 'TRANSACTION_ASSERT_COMMIT', 'TRANSACTION', 'commit assertion', '127.0.0.1'
);
COMMIT;

SELECT COUNT(*) = 1 AS commit_visible
FROM operation_log
WHERE action_type = 'TRANSACTION_ASSERT_COMMIT'
  AND description = 'commit assertion';

START TRANSACTION;
INSERT INTO operation_log (
    operator_user_id, action_type, target_type, description, ip_address
) VALUES (
    1, 'TRANSACTION_ASSERT_ROLLBACK', 'TRANSACTION', 'rollback assertion', '127.0.0.1'
);
ROLLBACK;

SELECT COUNT(*) = 0 AS rollback_invisible
FROM operation_log
WHERE action_type = 'TRANSACTION_ASSERT_ROLLBACK'
  AND description = 'rollback assertion';

DELETE FROM operation_log
WHERE action_type = 'TRANSACTION_ASSERT_COMMIT'
  AND description = 'commit assertion';

