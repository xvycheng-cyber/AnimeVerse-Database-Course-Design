USE anime_platform;

-- The procedures contain the canonical transaction demonstrations.
-- These raw statements show explicit COMMIT and ROLLBACK behavior.

START TRANSACTION;
INSERT INTO operation_log (
    operator_user_id, action_type, target_type, description, ip_address
) VALUES (
    1, 'RAW_TRANSACTION_COMMIT', 'TRANSACTION', 'explicit transaction commit demo', '127.0.0.1'
);
COMMIT;

START TRANSACTION;
INSERT INTO operation_log (
    operator_user_id, action_type, target_type, description, ip_address
) VALUES (
    1, 'RAW_TRANSACTION_ROLLBACK', 'TRANSACTION', 'explicit transaction rollback demo', '127.0.0.1'
);
ROLLBACK;

-- Exception rollback is verified through sp_transaction_demo('exception').

