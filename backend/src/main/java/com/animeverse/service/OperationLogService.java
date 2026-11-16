package com.animeverse.service;

import com.animeverse.entity.OperationLogEntity;
import com.animeverse.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public void record(
            Long userId,
            Long targetId,
            String module,
            String operation,
            String requestMethod,
            String requestUri,
            String ipAddress,
            int status,
            String errorMessage
    ) {
        try {
            String actionType = truncate(module + "_" + operation, 50);
            String description = requestMethod + " " + requestUri + " status=" + status;
            if (errorMessage != null && !errorMessage.isBlank()) {
                description += " error=" + errorMessage;
            }
            operationLogMapper.insert(OperationLogEntity.builder()
                    .operatorUserId(userId)
                    .actionType(actionType)
                    .targetType(truncate(module, 50))
                    .targetId(targetId)
                    .description(truncate(description, 1000))
                    .ipAddress(truncate(ipAddress, 45))
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (RuntimeException exception) {
            log.warn("Operation log could not be persisted: module={}, operation={}", module, operation, exception);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
