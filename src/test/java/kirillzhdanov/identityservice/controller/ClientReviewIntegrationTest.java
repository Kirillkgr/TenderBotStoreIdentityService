package kirillzhdanov.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.model.order.Order;
import kirillzhdanov.identityservice.model.order.OrderStatus;
import kirillzhdanov.identityservice.testutil.MembershipFixtures;
import kirillzhdanov.identityservice.testutil.TestOrderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class ClientReviewIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MembershipFixtures fixtures;

    @Autowired
    private TestOrderFactory orderFactory;

    @Test
    @DisplayName("CLIENT: отзыв только для COMPLETED заказа; дубликат запрещён")
    void client_review_flow() throws Exception {
        String clientUsername = "client-review-" + System.nanoTime();

        // Client: register and get CLIENT token
        Cookie clientLogin = fixtures.registerAndLogin(clientUsername);
        var clientCtx = fixtures.prepareRoleMembership(clientLogin, clientUsername, RoleMembership.CLIENT);
        Long clientMasterId = clientCtx.masterId();
        Cookie clientToken = clientCtx.cookie();
        assertThat(clientToken.getValue()).isNotBlank();

        // Create order for the client (brandId=1) in QUEUED status
        Order order = orderFactory.createOrderForClient(clientUsername, 1L);

        String goodReview = "{\"rating\":5,\"comment\":\"great\"}";

        // Attempt review before COMPLETED -> 409
        mockMvc.perform(post("/order/v1/orders/" + order.getId() + "/review")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(clientMasterId))
                        .cookie(clientToken)
                        .header("Authorization", "Bearer " + clientToken.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goodReview))
                .andExpect(status().isConflict());

        // Set status to COMPLETED directly via factory (faster and avoids staff membership wiring here)
        orderFactory.setStatus(order.getId(), OrderStatus.COMPLETED);

        // Now review should pass -> 204
        mockMvc.perform(post("/order/v1/orders/" + order.getId() + "/review")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(clientMasterId))
                        .cookie(clientToken)
                        .header("Authorization", "Bearer " + clientToken.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goodReview))
                .andExpect(status().isNoContent());

        // Duplicate review -> 409
        mockMvc.perform(post("/order/v1/orders/" + order.getId() + "/review")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(clientMasterId))
                        .cookie(clientToken)
                        .header("Authorization", "Bearer " + clientToken.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goodReview))
                .andExpect(status().isConflict());
    }
}
