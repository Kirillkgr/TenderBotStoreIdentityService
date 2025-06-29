package kirillzhdanov.identityservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.*;

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

	private String telegramBotToken;

	@ManyToMany(mappedBy = "brands")
	private Set<User> users = new HashSet<>();
}
