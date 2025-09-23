package kirillzhdanov.identityservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import kirillzhdanov.identityservice.dto.*;
import kirillzhdanov.identityservice.service.AuthService;
import kirillzhdanov.identityservice.service.CookieService;
import kirillzhdanov.identityservice.util.Base64Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/auth/v1")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

    private final CookieService cookieService;

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

        // Создаем куки с refresh token через сервис
        ResponseCookie refreshTokenCookie = cookieService.buildRefreshCookie(response.getRefreshToken());

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

        // Установим новый refresh как HttpOnly cookie
        ResponseCookie refreshCookie = cookieService.buildRefreshCookie(response.getRefreshToken());

        // Не возвращаем refresh в теле
        response.setRefreshToken(null);

        // Очистим JSESSIONID (оба варианта: с доменом и без)
        List<ResponseCookie> jsessionClears = cookieService.buildClearSessionCookies();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, jsessionClears.get(0).toString())
                .header(HttpHeaders.SET_COOKIE, jsessionClears.get(1).toString())
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

        // Очистить refresh cookie (оба варианта) и JSESSIONID
        List<ResponseCookie> refreshClears = cookieService.buildClearRefreshCookies();
        List<ResponseCookie> jsessionClears = cookieService.buildClearSessionCookies();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshClears.get(0).toString())
                .header(HttpHeaders.SET_COOKIE, refreshClears.get(1).toString())
                .header(HttpHeaders.SET_COOKIE, jsessionClears.get(0).toString())
                .header(HttpHeaders.SET_COOKIE, jsessionClears.get(1).toString())
                .build();
    }

    @DeleteMapping("/logout/all/{username}")
    public ResponseEntity<Void> logoutAll(@PathVariable String username) {
        authService.revokeAllUserTokens(username);
        return ResponseEntity.ok().build();
    }
}
