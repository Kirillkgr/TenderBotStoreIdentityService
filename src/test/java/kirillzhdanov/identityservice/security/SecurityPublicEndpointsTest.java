package kirillzhdanov.identityservice.security;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SecurityPublicEndpointsTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private MasterAccountRepository masterAccountRepository;

    @BeforeEach
    void seed() {
        MasterAccount m = new MasterAccount();
        m.setName("M");
        m = masterAccountRepository.save(m);
        String uniq = "PublicBrand-" + System.nanoTime();
        Brand b = Brand.builder().name(uniq).build();
        b.setMaster(m);
        brandRepository.save(b);
    }

    @Test
    void menu_brands_is_public_without_context() {
        ResponseEntity<String> resp = rest.getForEntity("/menu/v1/brands", String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody() != null && resp.getBody().contains("PublicBrand"));
    }

    @Test
    void auth_brands_requires_auth_or_context() {
        ResponseEntity<String> resp = rest.getForEntity("/auth/v1/brands", String.class);
        // Ожидаем 401 без JWT/контекста (по SecurityConfig)
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }
}
