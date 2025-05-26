package kirillzhdanov.identityservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.LoginRequest;
import kirillzhdanov.identityservice.dto.LoginResponse;
import kirillzhdanov.identityservice.dto.TokenRefreshRequest;
import kirillzhdanov.identityservice.dto.TokenRefreshResponse;
import kirillzhdanov.identityservice.dto.UserRegistrationRequest;
import kirillzhdanov.identityservice.dto.UserResponse;
import kirillzhdanov.identityservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Аутентификация", description = "API для регистрации и входа пользователей")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@Operation(summary = "Регистрация нового пользователя", description = "Создает нового пользователя в системе")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Пользователь успешно зарегистрирован",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
			@ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
			@ApiResponse(responseCode = "409", description = "Пользователь с таким email уже существует")
	})
	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegistrationRequest request){

		UserResponse response = authService.registerUser(request);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Вход пользователя", description = "Аутентификация пользователя и получение JWT токенов")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Успешная аутентификация",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))),
			@ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
			@ApiResponse(responseCode = "401", description = "Неверные учетные данные")
	})
	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request){

		LoginResponse response = authService.login(request);
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
			@ApiResponse(responseCode = "403", description = "Доступ запрещен")
	})
	@PostMapping("/revoke")
	@PreAuthorize("hasAuthority('USER')")
	public ResponseEntity<Void> revokeToken(@RequestParam String token){

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
	@PreAuthorize("hasAuthority('ADMIN')")
	public ResponseEntity<Void> revokeAllUserTokens(@RequestParam String username){

		authService.revokeAllUserTokens(username);
		return ResponseEntity.ok().build();
	}
}
