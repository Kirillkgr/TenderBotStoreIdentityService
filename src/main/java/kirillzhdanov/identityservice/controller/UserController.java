package kirillzhdanov.identityservice.controller;

import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.*;
import kirillzhdanov.identityservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    @PostMapping("/email/verified")
    public ResponseEntity<EmailVerifiedResponse> isEmailVerified(@Valid @RequestBody EmailVerificationRequest req,
                                                                 Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).build();
        }
        var resp = userProfileService.checkEmailVerified(authentication.getName(), req.getEmail());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/verifield/email")
    public ResponseEntity<Void> requestEmailCode(@Valid @RequestBody EmailVerificationRequest req,
                                                 Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).build();
        }
        userProfileService.sendVerificationCode(authentication.getName(), req.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/verifield/email")
    public ResponseEntity<EmailVerifiedResponse> verifyEmailCode(@Valid @RequestBody EmailVerificationRequest req,
                                                                 Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).build();
        }
        var resp = userProfileService.verifyCode(authentication.getName(), req);
        return ResponseEntity.ok(resp);
    }

    @PatchMapping("/edit")
    public ResponseEntity<UserResponse> editProfile(@Valid @RequestBody UpdateUserRequest request,
                                                    Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).build();
        }
        UserResponse resp = userProfileService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/avatar")
    public ResponseEntity<AvatarUploadResponse> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                             Authentication authentication) throws java.io.IOException {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).build();
        }
        AvatarUploadResponse resp = userProfileService.uploadAvatar(authentication.getName(), file);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/avatar")
    public ResponseEntity<byte[]> getOwnAvatar(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).build();
        }
        // username is unique, load id from profile service
        // For simplicity, UserProfileService will resolve via username inside method, but we use dedicated helper by id
        try {
            Long userId = Long.parseLong(authentication.getName());
            byte[] bytes = userProfileService.getAvatarBytes(userId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=60")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(bytes);
        } catch (NumberFormatException e) {
            // If principal name is not numeric, resolve inside service by username
            // Reuse upload path key derivation by looking up user from username
            byte[] bytes = userProfileService.getAvatarBytesByUsername(authentication.getName());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=60")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(bytes);
        }
    }
}
