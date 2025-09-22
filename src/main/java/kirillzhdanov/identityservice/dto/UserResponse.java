package kirillzhdanov.identityservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;

    private String username;

    private String firstName;
    private String lastName;
    private String patronymic;
    private LocalDate dateOfBirth;

    private String email;

    private String phone;

    private String avatarUrl;

    private Boolean emailVerified;

    private Set<String> roles;

    private Set<BrandDto> brands;

    private String accessToken;

    @JsonIgnore
    private String refreshToken;
}
