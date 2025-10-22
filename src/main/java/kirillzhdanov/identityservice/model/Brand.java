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

	// Public-facing domain label for subdomain routing (Unicode allowed). Must be unique.
	@Column(unique = true)
	private String domain;

	@Column(name = "description", length = 2048)
	private String description;

	@ManyToOne(fetch = FetchType.LAZY)
	@JsonIgnore
	private MasterAccount master;

	private String telegramBotToken;

	@ManyToMany(mappedBy = "brands")
	@Builder.Default
    @JsonIgnore
	private Set<User> users = new HashSet<>();

	@Column(name = "created_at", updatable = false)
	private java.time.LocalDateTime createdAt;

	@Column(name = "updated_at")
	private java.time.LocalDateTime updatedAt;

	@PrePersist
	private void onCreate() {
		if (createdAt == null) createdAt = java.time.LocalDateTime.now();
		if (updatedAt == null) updatedAt = createdAt;
	}

	@PreUpdate
	private void onUpdate() {
		updatedAt = java.time.LocalDateTime.now();
	}
}
