package kirillzhdanov.identityservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvatarUploadResponse {
    private String avatarUrl; // backend-served URL
    private String key;       // S3 key for internal reference
}
