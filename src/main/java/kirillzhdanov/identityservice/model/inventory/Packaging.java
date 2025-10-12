package kirillzhdanov.identityservice.model.inventory;

import jakarta.persistence.*;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "packagings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_packagings_master_name", columnNames = {"master_id", "name"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Packaging {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_id", nullable = false)
    private MasterAccount master;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(name = "size", precision = 18, scale = 6, nullable = false)
    private BigDecimal size;
}
