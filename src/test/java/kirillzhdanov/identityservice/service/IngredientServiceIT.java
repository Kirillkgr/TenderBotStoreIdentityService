package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.config.IntegrationTestBase;
import kirillzhdanov.identityservice.dto.inventory.CreateIngredientRequest;
import kirillzhdanov.identityservice.dto.inventory.IngredientDto;
import kirillzhdanov.identityservice.dto.inventory.StockRowDto;
import kirillzhdanov.identityservice.model.master.RoleMembership;
import kirillzhdanov.identityservice.tenant.TenantContext;
import kirillzhdanov.identityservice.testutil.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class IngredientServiceIT extends IntegrationTestBase {

    @Autowired
    IngredientService ingredientService;
    @Autowired
    StockService stockService;
    @Autowired
    TestFixtures fx;

    @Test
    @WithMockUser(username = "owner", roles = {"OWNER"})
    void create_with_initialQty_creates_posted_supply_and_stock() {
        long w = fx.warehouse("Склад A");
        long u = fx.unit("кг");
        Long masterId = fx.ensureMaster();
        TenantContext.setMasterId(masterId);
        TenantContext.setRole(RoleMembership.OWNER);

        CreateIngredientRequest req = new CreateIngredientRequest();
        req.setName("картофель");
        req.setUnitId(u);
        req.setPackageSize(new BigDecimal("1.000"));
        req.setNotes(null);
        req.setWarehouseId(w);
        req.setInitialQty(new BigDecimal("3.500"));

        IngredientDto dto = ingredientService.create(req);
        assertThat(dto.getId()).isNotNull();

        List<StockRowDto> rows = stockService.listByWarehouse(w);
        StockRowDto row = rows.stream().filter(r -> r.getIngredientId().equals(dto.getId())).findFirst().orElse(null);
        assertThat(row).isNotNull();
        assertThat(row.getQuantity()).isEqualByComparingTo(new BigDecimal("3.500"));
    }

    @Test
    @WithMockUser(username = "owner", roles = {"OWNER"})
    void create_without_initialQty_does_not_create_stock() {
        long w = fx.warehouse("Склад B");
        long u = fx.unit("кг");
        Long masterId = fx.ensureMaster();
        TenantContext.setMasterId(masterId);
        TenantContext.setRole(RoleMembership.OWNER);

        CreateIngredientRequest req = new CreateIngredientRequest();
        req.setName("лук");
        req.setUnitId(u);
        req.setPackageSize(null);
        req.setNotes(null);
        // нет initialQty и warehouseId

        IngredientDto dto = ingredientService.create(req);
        assertThat(dto.getId()).isNotNull();

        List<StockRowDto> rows = stockService.listByWarehouse(w);
        boolean exists = rows.stream().anyMatch(r -> r.getIngredientId().equals(dto.getId()));
        assertThat(exists).isFalse();
    }
}
