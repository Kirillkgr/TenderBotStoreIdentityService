package kirillzhdanov.identityservice.model.pickup;

import jakarta.persistence.*;
import kirillzhdanov.identityservice.model.Brand;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;

@Entity
@Table(name = "pickup_points")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PickupPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    @JsonIgnore
    private Brand brand;

    @Column(name = "name")
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "active")
    private Boolean active;
}
