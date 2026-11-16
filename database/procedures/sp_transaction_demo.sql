USE anime_platform;
DROP PROCEDURE IF EXISTS sp_transaction_demo;
DELIMITER $$
CREATE PROCEDURE sp_transaction_demo(
    IN p_operator_user_id BIGINT UNSIGNED,
    IN p_mode VARCHAR(16),
    OUT p_result VARCHAR(255)
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF p_mode IS NULL OR p_mode NOT IN ('commit', 'rollback', 'exception') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'mode must be commit, rollback or exception';
    END IF;

    START TRANSACTION;

    INSERT INTO operation_log (
        operator_user_id, action_type, target_type, target_id, description, ip_address
    )
    VALUES (
        p_operator_user_id,
        CONCAT('TRANSACTION_DEMO_', UPPER(p_mode)),
        'TRANSACTION',
        NULL,
        CONCAT('transaction demo mode=', p_mode),
        '127.0.0.1'
    );

    IF p_mode = 'commit' THEN
        COMMIT;
        SET p_result = 'committed';
    ELSEIF p_mode = 'rollback' THEN
        ROLLBACK;
        SET p_result = 'rolled_back';
    ELSE
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'intentional transaction exception for rollback demonstration';
    END IF;
END$$
DELIMITER ;

