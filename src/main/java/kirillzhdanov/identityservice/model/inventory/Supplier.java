package kirillzhdanov.identityservice.model.inventory;

import jakarta.persistence.*;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_id", nullable = false)
    private MasterAccount master;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "phone", length = 64)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address", length = 512)
    private String address;
}
