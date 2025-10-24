package kirillzhdanov.identityservice.controller;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.product.Product;
import kirillzhdanov.identityservice.model.tags.GroupTag;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.GroupTagRepository;
import kirillzhdanov.identityservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
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
    @Autowired
    GroupTagRepository groupTagRepository;
    @Autowired
    ProductRepository productRepository;

    @BeforeEach
    void seedPublicData() {
        // Ensure at least one brand exists
        Brand brand = brandRepository.findAll().stream().findFirst().orElseGet(() -> {
            Brand b = new Brand();
            b.setName("PublicBrand");
            b.setOrganizationName("PublicOrg");
            return brandRepository.save(b);
        });

        // Ensure at least one root tag under the brand
        boolean hasAnyTag = groupTagRepository.findByBrandAndParentIsNull(brand).stream().findAny().isPresent();
        if (!hasAnyTag) {
            GroupTag root = new GroupTag("Root", brand, null);
            groupTagRepository.save(root);
        }

        // Ensure at least one visible product under the brand (root level)
        boolean hasAnyVisible = productRepository.findByBrandAndGroupTagIsNullAndVisibleIsTrue(brand).stream().findAny().isPresent();
        if (!hasAnyVisible) {
            Product p = new Product();
            p.setName("Public Product");
            p.setDescription("Visible");
            p.setPrice(new java.math.BigDecimal("9.99"));
            p.setVisible(true);
            p.setBrand(brand);
            // groupTag left null => root level
            productRepository.save(p);
        }
    }

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
        mvc.perform(get("/menu/v1/brands/{id}/tags", resolveBrandId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /menu/v1/brands/{id}/products is public")
    void products_public() throws Exception {
        mvc.perform(get("/menu/v1/brands/{id}/products", resolveBrandId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Aliases under /menu/v1 are public")
    void aliases_public() throws Exception {
        mvc.perform(get("/menu/v1/products/by-brand/{id}", resolveBrandId()))
                .andExpect(status().isOk());
        mvc.perform(get("/menu/v1/tags/by-brand/{id}", resolveBrandId()))
                .andExpect(status().isOk());
        mvc.perform(get("/menu/v1/group-tags/by-brand/{id}", resolveBrandId()))
                .andExpect(status().isOk());
    }
}
