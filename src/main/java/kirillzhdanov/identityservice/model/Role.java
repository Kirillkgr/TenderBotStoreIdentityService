package kirillzhdanov.identityservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Role {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(unique = true)
	@EqualsAndHashCode.Include
	private RoleName name;

	@ManyToMany(mappedBy = "roles")
	@Builder.Default
	private Set<User> users = new HashSet<>();

	public enum RoleName {
		OWNER, ADMIN, USER
	}
}
