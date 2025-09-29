package kirillzhdanov.identityservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "brands")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Brand {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@Column(unique = true)
	@EqualsAndHashCode.Include
	private String name;

	@Column(unique = true)
	@EqualsAndHashCode.Include
	private String organizationName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "master_id")
	@JsonIgnore
	private MasterAccount master;

	private String telegramBotToken;

	@ManyToMany(mappedBy = "brands")
	@Builder.Default
    @JsonIgnore
	private Set<User> users = new HashSet<>();
}
