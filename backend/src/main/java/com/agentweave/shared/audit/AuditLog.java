package com.agentweave.shared.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    AuditEventType eventType();

    String resourceType();

    String resourceId() default "";

    boolean includeRequest() default true;

    boolean includeResponse() default true;
}
