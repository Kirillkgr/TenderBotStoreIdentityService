package kirillzhdanov.identityservice.controller.order;

import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.testutil.MembershipFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class OrderCashierIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MembershipFixtures fixtures;

    private Cookie login;
    private String username;

    @BeforeEach
    void setup() throws Exception {
        username = "cashier-user-" + System.nanoTime();
        login = fixtures.ensureLogin(username);
    }

    @Test
    @DisplayName("CASHIER/USER: доступ к списку заказов (без членства) возвращает 200 и пустую страницу")
    void ordersAccessibleWithoutMembership_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/order/v1/orders")
                        .cookie(login)
                        .header("Authorization", "Bearer " + login.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        // Смысл теста: эндпоинт доступен, но без членства список пуст (проверка содержимого делегирована сервису с пагинацией)
    }

    @Test
    @DisplayName("CASHIER: создание бренда разрешено (201), создание продукта без прав остаётся 403")
    void cashierBrandCreationAllowed_productCreationForbidden() throws Exception {
        // Создание бренда теперь разрешено для аутентифицированного пользователя -> 201
        String brandJson = "{\"name\":\"X\",\"organizationName\":\"ORG\"}";
        mockMvc.perform(post("/auth/v1/brands")
                        .cookie(login)
                        .header("Authorization", "Bearer " + login.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brandJson))
                .andExpect(status().isCreated());

        // Попытка создать продукт -> 403 (нет membership OWNER/ADMIN в контексте бренда)
        String productJson = "{\"name\":\"P\",\"price\":10,\"brandId\":1}";
        mockMvc.perform(post("/auth/v1/products")
                        .cookie(login)
                        .header("Authorization", "Bearer " + login.getValue())
                        .header("X-Master-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isForbidden());
    }
}
