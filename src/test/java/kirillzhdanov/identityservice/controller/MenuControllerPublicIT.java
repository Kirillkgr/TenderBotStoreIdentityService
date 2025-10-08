package kirillzhdanov.identityservice.controller;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.repository.BrandRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Public: MenuController endpoints")
@Tag("public-menu")
class MenuControllerPublicIT extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    BrandRepository brandRepository;

    @Test
    @DisplayName("GET /menu/v1/brands is public")
    void brands_public() throws Exception {
        mvc.perform(get("/menu/v1/brands"))
                .andExpect(status().isOk());
    }

    private long resolveBrandId() {
        return brandRepository.findAll().stream()
                .findFirst()
                .map(Brand::getId)
                .orElse(1L);
    }

    @Test
    @DisplayName("GET /menu/v1/brands/{id} is public")
    void brand_public() throws Exception {
        mvc.perform(get("/menu/v1/brands/{id}", resolveBrandId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /menu/v1/brands/{id}/tags is public")
    void tags_public() throws Exception {
        mvc.perform(get("/menu/v1/brands/{id}/tags", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /menu/v1/brands/{id}/products is public")
    void products_public() throws Exception {
        mvc.perform(get("/menu/v1/brands/{id}/products", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Aliases under /menu/v1 are public")
    void aliases_public() throws Exception {
        mvc.perform(get("/menu/v1/products/by-brand/{id}", 1L))
                .andExpect(status().isOk());
        mvc.perform(get("/menu/v1/tags/by-brand/{id}", 1L))
                .andExpect(status().isOk());
        mvc.perform(get("/menu/v1/group-tags/by-brand/{id}", 1L))
                .andExpect(status().isOk());
    }
}
