package com.animeverse.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class OperationLogVO {

    private Long id;
    private Long operatorUserId;
    private String username;
    private String actionType;
    private String targetType;
    private Long targetId;
    private String description;
    private String ipAddress;
    private LocalDateTime createdAt;

    public Long getUserId() {
        return operatorUserId;
    }

    public String getModule() {
        return targetType;
    }

    public String getOperation() {
        return actionType;
    }

    public String getAction() {
        return actionType;
    }

    public String getMethod() {
        return descriptionPart(0);
    }

    public String getPath() {
        return descriptionPart(1);
    }

    public String getIp() {
        return ipAddress;
    }

    public Boolean getSuccess() {
        return description != null && !description.contains("status=0");
    }

    public String getErrorMessage() {
        if (description == null) {
            return null;
        }
        int index = description.indexOf("error=");
        return index < 0 || index + 6 >= description.length()
                ? null
                : description.substring(index + 6);
    }

    private String descriptionPart(int index) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String[] parts = description.split("\\s+", 3);
        return index < parts.length ? parts[index] : null;
    }
}
