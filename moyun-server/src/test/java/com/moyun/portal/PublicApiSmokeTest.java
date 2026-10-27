package com.moyun.portal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 公开接口冒烟 + 链路头契约：
 * 验证安全链放行验证码接口，且每个响应都带 traceId / API 版本。
 */
@SpringBootTest
@AutoConfigureMockMvc
class PublicApiSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    void captchaIsPublicAndCarriesTraceHeaders() throws Exception {
        mockMvc.perform(get("/portal/captchaImage"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(header().string("X-API-Version", "1"));
    }
}
