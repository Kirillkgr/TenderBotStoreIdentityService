package kirillzhdanov.identityservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import kirillzhdanov.identityservice.dto.*;
import kirillzhdanov.identityservice.service.AuthService;
import kirillzhdanov.identityservice.util.Base64Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	/* Registration endpoint */
	@PostMapping("/register")
	public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegistrationRequest request) {

		UserResponse response = authService.registerUser(request);
		return ResponseEntity.status(201)
							 .body(response);
	}

	/* Login endpoint */
	@PostMapping("/login")
	public ResponseEntity<UserResponse> login(@NotEmpty @NotBlank @RequestHeader("Authorization") String authHeader) {
		LoginRequest requestFromAuthHeader = Base64Utils.getUsernameAndPassword(authHeader);
		UserResponse response = authService.login(requestFromAuthHeader);
		return ResponseEntity.ok(response);
	}


	/* Refresh token endpoint */
	@PostMapping("/refresh")
	public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {

		TokenRefreshResponse response = authService.refreshToken(request);
		return ResponseEntity.ok(response);
	}

	/* Revoke token endpoint */
	@PostMapping("/revoke")
	public ResponseEntity<Void> revokeToken(@RequestParam String token) {
		// Проверка аутентификации пользователя
		Authentication authentication = SecurityContextHolder.getContext()
															 .getAuthentication();
		if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
								 .build();
		}

		authService.revokeToken(token);
		return ResponseEntity.ok()
							 .build();
	}

	/* Revoke all tokens endpoint */
	@PostMapping("/revoke-all")
	public ResponseEntity<Void> revokeAllUserTokens(@RequestParam String username) {
		// Проверка роли ADMIN
		Authentication authentication = SecurityContextHolder.getContext()
															 .getAuthentication();
		boolean isAdmin = authentication != null && authentication.getAuthorities()
																  .stream()
																  .anyMatch(a -> a.getAuthority()
																				  .equals("ROLE_ADMIN"));

		if (!isAdmin) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
								 .build();
		}

		authService.revokeAllUserTokens(username);
		return ResponseEntity.ok()
							 .build();
	}

	@DeleteMapping("/logout")
	public ResponseEntity<Void> logout(@RequestBody String token) {

		authService.revokeToken(token);
		return ResponseEntity.ok()
							 .build();
	}

	@DeleteMapping("/logout/all/{username}")
	public ResponseEntity<Void> logoutAll(@PathVariable String username) {

		authService.revokeAllUserTokens(username);
		return ResponseEntity.ok()
							 .build();
	}
}
