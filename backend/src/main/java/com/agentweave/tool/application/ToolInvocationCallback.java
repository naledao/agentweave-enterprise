package com.agentweave.tool.application;

@FunctionalInterface
public interface ToolInvocationCallback {

    Object proceed() throws Throwable;
}
