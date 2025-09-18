package kirillzhdanov.identityservice.controller;

import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.EmailVerificationRequest;
import kirillzhdanov.identityservice.dto.EmailVerifiedResponse;
import kirillzhdanov.identityservice.dto.UpdateUserRequest;
import kirillzhdanov.identityservice.dto.UserResponse;
import kirillzhdanov.identityservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
        var resp = userProfileService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(resp);
    }
}
