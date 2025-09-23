package kirillzhdanov.identityservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true)
	private String username;

	private String password;

	private String firstName;
	private String lastName;

	private String patronymic;
	private LocalDate dateOfBirth;

	@Column(unique = true)
	private String email;

	private String phone;

    // URL аватара (например, из Google)
    private String avatarUrl;

    private boolean emailVerified;

    private String emailVerificationCode;

    private LocalDateTime emailVerificationExpiresAt;

    private String pendingEmail;

    // Ссылка на мастера (создателя/владельца) пользователя
    @Column(name = "master_id")
    private Long masterId;

    // Отдел (many-to-one)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    // Дата создания для аудита/сортировки
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

	@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
	@JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
	private Set<Role> roles = new HashSet<>();

	@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
	@JoinTable(name = "user_brand", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "brand_id"))
    @Builder.Default
	private Set<Brand> brands = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

