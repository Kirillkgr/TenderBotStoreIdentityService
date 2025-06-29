package kirillzhdanov.identityservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Token {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, length = 1024)
	private String token;

	@Enumerated(EnumType.STRING)
	private TokenType tokenType;

	private boolean revoked;

	private LocalDateTime expiryDate;

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

	public boolean isValid() {

		return !revoked && expiryDate.isAfter(LocalDateTime.now());
	}

	public enum TokenType {
		ACCESS, REFRESH
	}
}
