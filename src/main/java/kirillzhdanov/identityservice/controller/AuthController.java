package kirillzhdanov.identityservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import kirillzhdanov.identityservice.dto.*;
import kirillzhdanov.identityservice.service.AuthService;
import kirillzhdanov.identityservice.util.Base64Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth/v1")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@Value("${server.servlet.session.cookie.domain:}")
	private String cookieDomain;

	@Value("${jwt.refresh.expiration:2592000000}")
	private long refreshExpirationMs;

	/* Registration endpoint */
	@PostMapping("/checkUsername")
	public ResponseEntity<UserResponse> checkUsername(@Valid @NotEmpty @NotBlank @RequestParam String username) {
		boolean response;
		response = authService.checkUniqUsername(username);
		return ResponseEntity.status(response ? 409 : 200)
							 .build();
	}

	/* Convenience GET endpoint to support clients using GET for availability check */
	@GetMapping("/checkUsername")
	public ResponseEntity<UserResponse> checkUsernameGet(@Valid @NotEmpty @NotBlank @RequestParam String username) {
		boolean response = authService.checkUniqUsername(username);
		return ResponseEntity.status(response ? 409 : 200).build();
	}

	/* Registration endpoint */
	@PostMapping("/register")
	public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegistrationRequest request) {

		UserResponse response = authService.registerUser(request);
		return ResponseEntity.status(201)
							 .body(response);
	}

	/* Login endpoint */
	@PostMapping("/login")
	public ResponseEntity<UserResponse> login(
			@NotEmpty @NotBlank @RequestHeader("Authorization") String authHeader) {
		
		LoginRequest requestFromAuthHeader = Base64Utils.getUsernameAndPassword(authHeader);
		UserResponse response = authService.login(requestFromAuthHeader);

		// Создаем куки с refresh token (унификация с OAuth2 flow)
		ResponseCookie.ResponseCookieBuilder rcb = ResponseCookie.from("refreshToken", response.getRefreshToken())
				.httpOnly(true)
				.secure(true)
				.path("/")
				.sameSite("None");
		if (refreshExpirationMs > 0) {
			long seconds = Math.max(1, refreshExpirationMs / 1000);
			rcb.maxAge(java.time.Duration.ofSeconds(seconds));
		}
		if (cookieDomain != null && !cookieDomain.isBlank()) {
			rcb.domain(cookieDomain);
		}
		ResponseCookie refreshTokenCookie = rcb.build();
		
		// Удаляем refresh token из тела ответа
		response.setRefreshToken(null);
		
		// Возвращаем ответ с куки
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
			.body(response);
	}


	/* Refresh token endpoint */
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@CookieValue(name = "refreshToken", required = false) String refreshToken) {

		if (refreshToken == null || refreshToken.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TokenRefreshResponse.builder().build());
		}

		        TokenRefreshResponse response = authService.refreshToken(new TokenRefreshRequest(refreshToken));
		// Do not expose refreshToken in body (it is held in HttpOnly cookie)
		response.setRefreshToken(null);

		// Дополнительно очищаем возможный JSESSIONID (если был создан в процессе OAuth2)
		ResponseCookie jsessionCleared = clearSession();
		ResponseCookie jsessionClearedNoDomain = ResponseCookie.from("JSESSIONID", "")
				.httpOnly(true)
				.secure(true)
				.path("/")
				.maxAge(java.time.Duration.ZERO)
				.sameSite("None")
				.build();

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, jsessionCleared.toString())
				.header(HttpHeaders.SET_COOKIE, jsessionClearedNoDomain.toString())
				.body(response);
	}

	@GetMapping("/whoami")
	public ResponseEntity<UserResponse> whoami(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		String username = authentication.getName();
		UserResponse profile = authService.getUserProfile(username);
		return ResponseEntity.ok(profile);
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
	public ResponseEntity<Void> logout(@RequestBody String token,
                                       @CookieValue(name = "refreshToken", required = false) String refreshCookie) {

        // Revoke access token sent in body
        if (token != null && !token.isBlank()) {
            authService.revokeToken(token);
        }

        // Revoke refresh token if present in cookie
        if (refreshCookie != null && !refreshCookie.isBlank()) {
            authService.revokeToken(refreshCookie);
        }

		// Instruct client to clear refresh cookie (attributes must match original)
		ResponseCookie.ResponseCookieBuilder refreshClearBuilder = ResponseCookie.from("refreshToken", "")
				.httpOnly(true)
				.secure(true)
				.path("/")
				.maxAge(java.time.Duration.ZERO)
				.sameSite("None");
		if (cookieDomain != null && !cookieDomain.isBlank()) {
			refreshClearBuilder.domain(cookieDomain);
		}
		ResponseCookie refreshCleared = refreshClearBuilder.build();
		// Also clear host-only variant (without Domain) in case it was set that way earlier
		ResponseCookie refreshClearedNoDomain = ResponseCookie.from("refreshToken", "")
				.httpOnly(true)
				.secure(true)
				.path("/")
				.maxAge(java.time.Duration.ZERO)
				.sameSite("None")
				.build();

		// Also instruct client to clear JSESSIONID if it exists

		ResponseCookie jsessionCleared = clearSession();
		// Also clear host-only variant for JSESSIONID
		ResponseCookie jsessionClearedNoDomain = ResponseCookie.from("JSESSIONID", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(0)
				.sameSite("None")
            .build();

        return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshCleared.toString())
				.header(HttpHeaders.SET_COOKIE, refreshClearedNoDomain.toString())
				.header(HttpHeaders.SET_COOKIE, jsessionCleared.toString())
				.header(HttpHeaders.SET_COOKIE, jsessionClearedNoDomain.toString())
                             .build();
    }

	private ResponseCookie clearSession() {
		ResponseCookie.ResponseCookieBuilder jSessionClearBuilder = ResponseCookie.from("JSESSIONID", "")
				.httpOnly(true)
				.secure(true)
				.path("/")
				.maxAge(java.time.Duration.ZERO)
				.sameSite("None");
		if (cookieDomain != null && !cookieDomain.isBlank()) {
			jSessionClearBuilder.domain(cookieDomain);
		}
		return jSessionClearBuilder.build();
	}

	@DeleteMapping("/logout/all/{username}")
	public ResponseEntity<Void> logoutAll(@PathVariable String username) {

		authService.revokeAllUserTokens(username);
		return ResponseEntity.ok()
							 .build();
	}
}
