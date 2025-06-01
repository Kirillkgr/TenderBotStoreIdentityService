package kirillzhdanov.identityservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

	@Operation(summary = "Регистрация нового пользователя", description = "Создает нового пользователя в системе")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
			@ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
			@ApiResponse(responseCode = "409", description = "Пользователь с таким именем уже существует")
	})
	@PostMapping("/register")
	public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegistrationRequest request){

		UserResponse response = authService.registerUser(request);
		return ResponseEntity.status(201).body(response);
	}

	@Operation(summary = "Вход пользователя", description = "Аутентификация пользователя и получение JWT токенов")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Успешная аутентификация",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
			@ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
			@ApiResponse(responseCode = "401", description = "Неверные учетные данные")
	})
	@PostMapping("/login")
	public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request){

		UserResponse response = authService.login(request);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Обновление токена", description = "Обновление токена доступа с помощью токена обновления")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Токен успешно обновлен",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenRefreshResponse.class))),
			@ApiResponse(responseCode = "403", description = "Токен обновления недействителен или истек")
	})
	@PostMapping("/refresh")
	public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request){

		TokenRefreshResponse response = authService.refreshToken(request);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Отзыв токена", description = "Отзыв указанного токена")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Токен успешно отозван"),
			@ApiResponse(responseCode = "401", description = "Не авторизован")
	})
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

	@Operation(summary = "Отзыв всех токенов пользователя", description = "Отзыв всех токенов указанного пользователя")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Все токены пользователя успешно отозваны"),
			@ApiResponse(responseCode = "403", description = "Доступ запрещен"),
			@ApiResponse(responseCode = "404", description = "Пользователь не найден")
	})
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
