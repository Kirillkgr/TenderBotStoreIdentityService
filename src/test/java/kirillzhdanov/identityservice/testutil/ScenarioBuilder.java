package kirillzhdanov.identityservice.testutil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kirillzhdanov.identityservice.dto.BrandDto;
import kirillzhdanov.identityservice.dto.product.ProductCreateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Вспомогательный билдер сценариев: быстрые создания Brand/Product через реальные API.
 */
@Component
public class ScenarioBuilder {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Создаёт бренд и возвращает его id.
     */
    public long createBrand(Cookie ctxCookie, Long masterId, String name, String orgName) throws Exception {
        BrandDto dto = BrandDto.builder().name(name).organizationName(orgName).build();
        MvcResult res = mockMvc.perform(post("/auth/v1/brands")
                        .cookie(ctxCookie)
                        .header("X-Master-Id", masterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(res.getResponse().getContentAsString());
        assertThat(node.has("id")).isTrue();
        return node.get("id").asLong();
    }

    /**
     * Создаёт продукт и возвращает его id.
     */
    public long createProduct(Cookie ctxCookie, String name, BigDecimal price, long brandId) throws Exception {
        ProductCreateRequest req = new ProductCreateRequest();
        req.setName(name);
        req.setPrice(price);
        req.setBrandId(brandId);
        MvcResult res = mockMvc.perform(post("/auth/v1/products")
                        .cookie(ctxCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(res.getResponse().getContentAsString());
        assertThat(node.has("id")).isTrue();
        return node.get("id").asLong();
    }
}
