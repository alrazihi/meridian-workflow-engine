package com.meridian.infrastructure.audit;

import com.meridian.application.port.outbound.AuditService;
import com.meridian.domain.model.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class AuditAspect {

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @Before("@annotation(org.springframework.security.access.prepost.PreAuthorize)")
    public void auditSecurityCheck(JoinPoint joinPoint) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;

            HttpServletRequest request = attributes.getRequest();
            String actor = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous";
            String method = joinPoint.getSignature().toShortString();
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            Map<String, Object> details = new HashMap<>();
            details.put("method", method);
            details.put("args", joinPoint.getArgs());

            AuditLog auditLog = AuditLog.create(
                    actor,
                    "SECURITY_CHECK",
                    "METHOD",
                    method,
                    ipAddress,
                    userAgent,
                    details
            );
            auditService.log(auditLog);
        } catch (Exception e) {
            // Audit logging should not break the application
        }
    }
}
