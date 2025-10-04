package kirillzhdanov.identityservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.model.order.Order;
import kirillzhdanov.identityservice.testutil.MembershipFixtures;
import kirillzhdanov.identityservice.testutil.TestOrderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class ClientMessagingIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MembershipFixtures fixtures;

    @Autowired
    private TestOrderFactory orderFactory;

    @Test
    @DisplayName("CLIENT: двусторонние сообщения и long-poll ack")
    void client_messaging_and_longpoll_ack() throws Exception {
        String clientUsername = "client-" + System.nanoTime();
        String staffUsername = "staff-" + System.nanoTime();

        // Client: register and get CLIENT token
        Cookie clientLogin = fixtures.registerAndLogin(clientUsername);
        var clientCtx = fixtures.prepareRoleMembership(clientLogin, clientUsername, RoleMembership.CLIENT);
        Long clientMasterId = clientCtx.masterId();
        Cookie clientToken = clientCtx.cookie();

        // Staff: register and get ADMIN token
        Cookie staffLogin = fixtures.registerAndLogin(staffUsername);
        var staffCtx = fixtures.prepareRoleMembership(staffLogin, staffUsername, RoleMembership.ADMIN);
        Long staffMasterId = staffCtx.masterId();
        Cookie staffToken = staffCtx.cookie();

        // Create order for the client (brandId=1)
        Order order = orderFactory.createOrderForClient(clientUsername, 1L);

        // Client -> staff message
        String clientMsg = "{\"text\":\"hello from client\"}";
        mockMvc.perform(post("/order/v1/orders/" + order.getId() + "/client-message")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(clientMasterId))
                        .cookie(clientToken)
                        .header("Authorization", "Bearer " + clientToken.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientMsg))
                .andExpect(status().isNoContent());

        // Staff -> client message (triggers long-poll event for client)
        String staffMsg = "{\"text\":\"hello from staff\"}";
        mockMvc.perform(post("/order/v1/orders/" + order.getId() + "/message")
                        .header("X-Brand-Id", "1")
                        .header("X-Master-Id", String.valueOf(staffMasterId))
                        .cookie(staffToken)
                        .header("Authorization", "Bearer " + staffToken.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(staffMsg))
                .andExpect(status().isNoContent());

        // Client polls notifications (permitAll, but authenticated returns envelope)
        // Try a couple of quick polls until we see events
        long since = 0;
        JsonNode envelope = null;
        for (int i = 0; i < 5; i++) {
            MvcResult started = mockMvc.perform(get("/notifications/longpoll")
                            .param("since", String.valueOf(since))
                            .param("timeoutMs", "500")
                            .param("maxBatch", "10")
                            .cookie(clientToken)
                            .header("Authorization", "Bearer " + clientToken.getValue()))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            MvcResult completed = mockMvc.perform(asyncDispatch(started))
                    .andReturn();
            int sc = completed.getResponse().getStatus();
            if (sc == 200) {
                String content = completed.getResponse().getContentAsString();
                if (!content.isBlank()) {
                    envelope = objectMapper.readTree(content);
                    break;
                }
            }
        }
        assertThat(envelope).isNotNull();
        assertThat(envelope.get("events")).isNotNull();
        assertThat(envelope.get("events").isArray()).isTrue();
        assertThat(envelope.get("events").size()).isGreaterThan(0);
        long lastId = envelope.get("events").get(envelope.get("events").size() - 1).get("id").asLong();
        long nextSince = envelope.get("nextSince").asLong();
        assertThat(nextSince).isGreaterThanOrEqualTo(lastId);

        // Client ACK
        String ack = "{\"lastReceivedId\":" + lastId + "}";
        mockMvc.perform(post("/notifications/longpoll/ack")
                        .cookie(clientToken)
                        .header("Authorization", "Bearer " + clientToken.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ack))
                .andExpect(status().isNoContent());

        // Client polls again with since=nextSince -> expect no new events (204)
        // Some async paths may return 200 with empty body; accept that as well.
        MvcResult started2 = mockMvc.perform(get("/notifications/longpoll")
                        .param("since", String.valueOf(nextSince))
                        .param("timeoutMs", "300")
                        .param("maxBatch", "10")
                        .cookie(clientToken)
                        .header("Authorization", "Bearer " + clientToken.getValue()))
                .andExpect(request().asyncStarted())
                .andReturn();
        MvcResult completed2 = mockMvc.perform(asyncDispatch(started2)).andReturn();
        int sc2 = completed2.getResponse().getStatus();
        if (sc2 == 200) {
            String content2 = completed2.getResponse().getContentAsString();
            assertThat(content2.isBlank()).isTrue();
        } else {
            assertThat(sc2).isEqualTo(204);
        }
    }
}
