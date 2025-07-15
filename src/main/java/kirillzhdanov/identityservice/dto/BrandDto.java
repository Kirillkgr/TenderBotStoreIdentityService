package kirillzhdanov.identityservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandDto {

	private Long id;

	private String name;
	private String organizationName;
}
