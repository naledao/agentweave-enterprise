package com.agentweave.shared.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;

class TraceIdProviderTest {

    private final TraceIdProvider traceIdProvider = new TraceIdProvider();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldPreferTraceIdHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdProvider.TRACE_ID_HEADER, "trace-header");
        request.addHeader(TraceIdProvider.REQUEST_ID_HEADER, "request-header");

        String traceId = traceIdProvider.currentTraceId(request);

        assertThat(traceId).isEqualTo("trace-header");
        assertThat(MDC.get(TraceIdProvider.TRACE_ID_KEY)).isEqualTo("trace-header");
    }

    @Test
    void shouldUseRequestIdHeaderWhenTraceIdHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdProvider.REQUEST_ID_HEADER, "request-header");

        String traceId = traceIdProvider.currentTraceId(request);

        assertThat(traceId).isEqualTo("request-header");
        assertThat(MDC.get(TraceIdProvider.TRACE_ID_KEY)).isEqualTo("request-header");
    }

    @Test
    void shouldGenerateTraceIdWhenNoHeadersExist() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        String traceId = traceIdProvider.currentTraceId(request);

        assertThat(traceId).isNotBlank();
        assertThat(MDC.get(TraceIdProvider.TRACE_ID_KEY)).isEqualTo(traceId);
    }
}
