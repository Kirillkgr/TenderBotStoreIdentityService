package kirillzhdanov.identityservice.model.inventory;

import jakarta.persistence.*;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "supplies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supply {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_id", nullable = false)
    private MasterAccount master;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "supplier_id")
    private Long supplierId; // optional, пока как Long

    @Column(name = "date", nullable = false)
    private OffsetDateTime date;

    @Column(name = "notes", length = 1024)
    private String notes;

    @Column(name = "status", nullable = false, length = 16)
    private String status; // DRAFT|POSTED|CANCELED

    @OneToMany(mappedBy = "supply", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SupplyItem> items = new ArrayList<>();
}
