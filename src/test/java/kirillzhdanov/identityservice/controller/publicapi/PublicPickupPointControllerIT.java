package kirillzhdanov.identityservice.controller.publicapi;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Public: PublicPickupPointController")
@Tag("public-pickup")
class PublicPickupPointControllerIT extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("GET /public/v1/pickup-points is public")
    void list_is_public() throws Exception {
        mvc.perform(get("/public/v1/pickup-points"))
                .andExpect(status().isOk());
    }
}
