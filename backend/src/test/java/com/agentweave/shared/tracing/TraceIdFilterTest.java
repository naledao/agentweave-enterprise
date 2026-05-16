package com.agentweave.shared.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

    private final TraceIdFilter traceIdFilter = new TraceIdFilter(new TraceIdProvider());

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldSetResponseHeaderAndClearMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdProvider.TRACE_ID_HEADER, "trace-filter");
        MockHttpServletResponse response = new MockHttpServletResponse();

        traceIdFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(TraceIdProvider.TRACE_ID_HEADER)).isEqualTo("trace-filter");
        assertThat(MDC.get(TraceIdProvider.TRACE_ID_KEY)).isNull();
        assertThat(MDC.get(CorrelationContext.CONVERSATION_ID_KEY)).isNull();
        assertThat(MDC.get(CorrelationContext.MESSAGE_ID_KEY)).isNull();
    }
}
