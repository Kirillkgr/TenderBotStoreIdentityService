package kirillzhdanov.identityservice.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtUserDetailsResponse {

	private Long userId;

	private String username;

	private List<Long> brandIds;

	private List<String> roles;
}
