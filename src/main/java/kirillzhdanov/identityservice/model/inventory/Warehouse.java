package kirillzhdanov.identityservice.model.inventory;

import jakarta.persistence.*;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_id", nullable = false)
    private MasterAccount master;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    // Создание склада
    public Warehouse(MasterAccount master, String name) {
        this.master = master;
        this.name = name;
    }
}
