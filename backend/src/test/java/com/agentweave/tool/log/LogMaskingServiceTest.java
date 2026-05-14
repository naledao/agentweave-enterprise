package com.agentweave.tool.log;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogMaskingServiceTest {

    private final LogMaskingService logMaskingService = new LogMaskingService();

    @Test
    void masksSecretsPhonesAndIdCards() {
        String masked = logMaskingService.mask(
                "token=sk-demo password:plain Bearer abc.def.ghi phone=13812345678 id=110101199003078888");

        assertThat(masked)
                .contains("token=******")
                .contains("password:******")
                .contains("Bearer ******")
                .contains("138****5678")
                .contains("110101********8888")
                .doesNotContain("sk-demo")
                .doesNotContain("plain")
                .doesNotContain("abc.def.ghi")
                .doesNotContain("13812345678")
                .doesNotContain("110101199003078888");
    }
}
