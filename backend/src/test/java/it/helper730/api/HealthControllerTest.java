package it.helper730.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.helper730.config.I18nConfig;
import it.helper730.i18n.Messages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HealthControllerTest.
 *
 * The health endpoint is used by the load balancer to check if the app is up.
 *
 * @WebMvcTest starts only the web layer. The class needs to import (with
 *             the @Import annotation) the beans it uses, otherwise they are
 *             mocked and the test fails.
 */
@WebMvcTest(HealthController.class)
@Import({Messages.class, I18nConfig.class})
public class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsUpWithVersion() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").isNotEmpty());
    }
}
