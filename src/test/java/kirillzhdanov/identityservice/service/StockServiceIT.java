package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.inventory.StockRowDto;
import kirillzhdanov.identityservice.testutil.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@Transactional
class StockServiceIT extends IntegrationTestBase {

    @Autowired
    StockService stockService;
    @Autowired
    TestFixtures fx;

    @Test
    @WithMockUser(username = "owner", roles = {"OWNER"})
    void earliestExpiry_and_lastSupplyDate_calculated() {
        long w = fx.warehouse("Склад");
        long u = fx.unit("кг");
        long ing = fx.ingredient("сыр", u);

        // две поставки: разные expiry и даты поставки
        fx.postedSupply(w, ing, 1.0, LocalDate.parse("2025-12-31"), OffsetDateTime.parse("2025-10-10T10:00:00Z"));
        fx.postedSupply(w, ing, 1.0, LocalDate.parse("2025-11-30"), OffsetDateTime.parse("2025-10-12T10:00:00Z"));

        List<StockRowDto> rows = stockService.listByWarehouse(w);
        StockRowDto row = rows.stream().filter(r -> r.getIngredientId().equals(ing)).findFirst().orElse(null);
        assertThat(row).isNotNull();
        assertThat(row.getExpiryDate()).isEqualTo("2025-11-30");
        assertThat(row.getLastSupplyDate()).isEqualTo("2025-10-12");
    }
}
