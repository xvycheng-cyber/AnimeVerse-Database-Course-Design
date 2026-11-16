package com.animeverse.common;

import com.animeverse.security.AuthenticatedUser;
import com.animeverse.security.SecurityUtils;
import com.animeverse.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final Pattern NUMERIC_PATH_SEGMENT = Pattern.compile("(?:^|/)(\\d+)(?:/|$)");

    private final OperationLogService operationLogService;

    @Around("@annotation(operationLog)")
    public Object logOperation(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        try {
            Object result = joinPoint.proceed();
            save(operationLog, 1, null);
            return result;
        } catch (Throwable throwable) {
            save(operationLog, 0, abbreviate(throwable.getMessage()));
            throw throwable;
        }
    }

    private void save(OperationLog operationLog, int status, String errorMessage) {
        HttpServletRequest request = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            request = attributes.getRequest();
        }
        Long userId = null;
        try {
            AuthenticatedUser user = SecurityUtils.currentUser();
            userId = user.id();
        } catch (RuntimeException ignored) {
            // Authentication failures still produce an audit event when a protected method was reached.
        }
        operationLogService.record(
                userId,
                targetId(request),
                operationLog.module(),
                operationLog.action(),
                request == null ? null : request.getMethod(),
                request == null ? null : request.getRequestURI(),
                request == null ? null : request.getRemoteAddr(),
                status,
                errorMessage
        );
    }

    private Long targetId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Matcher matcher = NUMERIC_PATH_SEGMENT.matcher(request.getRequestURI());
        return matcher.find() ? Long.valueOf(matcher.group(1)) : null;
    }

    private String abbreviate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
