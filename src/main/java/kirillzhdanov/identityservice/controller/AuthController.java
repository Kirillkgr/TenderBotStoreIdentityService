package kirillzhdanov.identityservice.controller;

import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.LoginRequest;
import kirillzhdanov.identityservice.dto.TokenRefreshRequest;
import kirillzhdanov.identityservice.dto.TokenRefreshResponse;
import kirillzhdanov.identityservice.dto.UserRegistrationRequest;
import kirillzhdanov.identityservice.dto.UserResponse;
import kirillzhdanov.identityservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	/* Registration endpoint */
	@PostMapping("/register")
	public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegistrationRequest request){

		UserResponse response = authService.registerUser(request);
		return ResponseEntity.status(201).body(response);
	}

	/* Login endpoint */
	@PostMapping("/login")
	public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request){

		UserResponse response = authService.login(request);
		return ResponseEntity.ok(response);
	}

	/* Refresh token endpoint */
	@PostMapping("/refresh")
	public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request){

		TokenRefreshResponse response = authService.refreshToken(request);
		return ResponseEntity.ok(response);
	}

	/* Revoke token endpoint */
	@PostMapping("/revoke")
	public ResponseEntity<Void> revokeToken(@RequestParam String token){
		// Проверка аутентификации пользователя
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if(authentication == null || !authentication.isAuthenticated() ||
				   authentication instanceof AnonymousAuthenticationToken) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		authService.revokeToken(token);
		return ResponseEntity.ok().build();
	}

	/* Revoke all tokens endpoint */
	@PostMapping("/revoke-all")
	public ResponseEntity<Void> revokeAllUserTokens(@RequestParam String username){
		// Проверка роли ADMIN
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean isAdmin = authentication != null &&
								  authentication.getAuthorities().stream()
										  .anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));

		if(!isAdmin) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		authService.revokeAllUserTokens(username);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/logout")
	public ResponseEntity<Void> logout(@RequestBody String token){

		authService.revokeToken(token);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/logout/all/{username}")
	public ResponseEntity<Void> logoutAll(@PathVariable String username){

		authService.revokeAllUserTokens(username);
		return ResponseEntity.ok().build();
	}
}
