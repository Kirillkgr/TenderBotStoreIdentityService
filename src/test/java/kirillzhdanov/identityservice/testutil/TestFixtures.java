package kirillzhdanov.identityservice.testutil;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import kirillzhdanov.identityservice.dto.inventory.supply.CreateSupplyRequest;
import kirillzhdanov.identityservice.model.inventory.Ingredient;
import kirillzhdanov.identityservice.model.inventory.Unit;
import kirillzhdanov.identityservice.model.inventory.Warehouse;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.inventory.IngredientRepository;
import kirillzhdanov.identityservice.repository.inventory.UnitRepository;
import kirillzhdanov.identityservice.repository.inventory.WarehouseRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.service.SupplyService;
import kirillzhdanov.identityservice.tenant.ContextAccess;
import kirillzhdanov.identityservice.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class TestFixtures {

    @Autowired
    private EntityManager em;
    @Autowired
    private UnitRepository unitRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private SupplyService supplyService;
    @Autowired
    private MasterAccountRepository masterAccountRepository;

    @Transactional
    public long unit(String name) {
        Long masterId = ensureMaster();
        Unit u = new Unit();
        u.setMaster(em.getReference(MasterAccount.class, masterId));
        u.setName(name);
        u.setShortName(null);
        return unitRepository.save(u).getId();
    }

    @Transactional
    public long warehouse(String name) {
        Long masterId = ensureMaster();
        Warehouse w = new Warehouse();
        w.setMaster(em.getReference(MasterAccount.class, masterId));
        w.setName(name);
        return warehouseRepository.save(w).getId();
    }

    @Transactional
    public long ingredient(String name, long unitId) {
        Long masterId = ensureMaster();
        Unit unit = unitRepository.findByIdAndMaster_Id(unitId, masterId).orElseThrow();
        Ingredient ing = new Ingredient();
        ing.setMaster(em.getReference(MasterAccount.class, masterId));
        ing.setName(name);
        ing.setUnit(unit);
        ing.setPackageSize(null);
        ing.setNotes(null);
        return ingredientRepository.save(ing).getId();
    }

    @Transactional
    public long postedSupply(long warehouseId, long ingredientId, double qty, LocalDate expiresAt, OffsetDateTime date) {
        ensureMaster();
        CreateSupplyRequest req = new CreateSupplyRequest();
        req.setWarehouseId(warehouseId);
        req.setSupplierId(null);
        req.setDate(date != null ? date : OffsetDateTime.now());
        req.setNotes("test supply");
        CreateSupplyRequest.Item it = new CreateSupplyRequest.Item();
        it.setIngredientId(ingredientId);
        it.setQty(qty);
        it.setExpiresAt(expiresAt);
        req.setItems(List.of(it));
        var supply = supplyService.create(req);
        supplyService.post(supply.getId());
        return supply.getId();
    }

    /**
     * Ensure master exists for tests and set TenantContext accordingly.
     * Returns master id.
     */
    @Transactional
    public Long ensureMaster() {
        Long id = ContextAccess.getMasterIdOrNull();
        if (id != null) return id;
        // Create or get default master for tests
        MasterAccount m = masterAccountRepository.findByName("test-master").orElseGet(() -> {
            MasterAccount n = new MasterAccount();
            n.setName("test-master");
            n.setStatus("ACTIVE");
            return masterAccountRepository.save(n);
        });
        TenantContext.setMasterId(m.getId());
        return m.getId();
    }
}
