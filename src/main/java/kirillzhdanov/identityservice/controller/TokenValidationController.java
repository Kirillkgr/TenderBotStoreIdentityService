package kirillzhdanov.identityservice.controller;

import kirillzhdanov.identityservice.dto.JwtUserDetailsResponse;
import kirillzhdanov.identityservice.security.JwtUtils;
import kirillzhdanov.identityservice.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для валидации JWT токенов.
 *
 * <p>Предоставляет эндпоинты для проверки валидности токенов и получения информации о пользователе.</p>
 */
@RestController
@RequestMapping("/token/validate")
@RequiredArgsConstructor
@Slf4j
public class TokenValidationController {

	private final TokenService tokenService;

	private final JwtUtils jwtUtils;

	private final UserService userService;

	/**
	 * Валидирует JWT токен.
	 *
	 * <p>Проверяет наличие и корректность заголовка Authorization, подпись токена и отсутствие токена в списке отозванных.</p>
	 *
	 * @param authHeader заголовок Authorization, содержащий Bearer токен
	 * @return {@code ResponseEntity} с HTTP статусом:
	 * {@code 200 OK} - токен валиден;
	 * {@code 403 Forbidden} - токен невалиден (некорректный заголовок, неверная подпись, отозванный токен).
	 */
	@PostMapping
	public ResponseEntity<Void> validateToken(@RequestHeader("Authorization") String authHeader) {

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			log.warn("Получен запрос с некорректным заголовком Authorization");
			return ResponseEntity.status(403)
								 .build();
		}

		String token = authHeader.substring(7);

		// Проверяем подпись токена
		boolean isSignatureValid = jwtUtils.validateTokenSignature(token);
		if (!isSignatureValid) {
			log.warn("Токен имеет недействительную подпись");
			return ResponseEntity.status(403)
								 .build();
		}

		// Проверяем, не отозван ли токен
		boolean isTokenValid = tokenService.isTokenValid(token);
		if (!isTokenValid) {
			log.warn("Токен отозван или не найден в базе данных");
			return ResponseEntity.status(403)
								 .build();
		}

		log.info("Токен успешно валидирован");
		return ResponseEntity.ok()
							 .build();
	}

	/**
	 * Валидирует JWT токен и возвращает информацию о пользователе.
	 *
	 * <p>Проверяет наличие и корректность заголовка Authorization, подпись токена и отсутствие токена в списке отозванных.
	 * При успешной валидации возвращает данные пользователя.</p>
	 *
	 * @param authHeader заголовок Authorization, содержащий Bearer токен
	 * @return {@code ResponseEntity} с HTTP статусом:
	 * {@code 200 OK} - токен валиден, в теле ответа содержатся данные пользователя;
	 * {@code 403 Forbidden} - токен невалиден (некорректный заголовок, неверная подпись, отозванный токен) или пользователь не найден.
	 */
	@PostMapping("/details")
	public ResponseEntity<JwtUserDetailsResponse> validateTokenAndGetUserDetails(@RequestHeader("Authorization") String authHeader) {

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			log.warn("Получен запрос с некорректным заголовком Authorization");
			return ResponseEntity.status(403)
								 .build();
		}

		String token = authHeader.substring(7);

		// Проверяем подпись токена
		boolean isSignatureValid = jwtUtils.validateTokenSignature(token);
		if (!isSignatureValid) {
			log.warn("Токен имеет недействительную подпись");
			return ResponseEntity.status(403)
								 .build();
		}

		// Проверяем, не отозван ли токен
		boolean isTokenValid = tokenService.isTokenValid(token);
		if (!isTokenValid) {
			log.warn("Токен отозван или не найден в базе данных");
			return ResponseEntity.status(403)
								 .build();
		}

		// Получаем ID пользователя из токена
		Long userId = jwtUtils.extractUserId(token);
		if (userId == null) {
			log.warn("Не удалось извлечь ID пользователя из токена");
			return ResponseEntity.status(403)
								 .build();
		}

		// Получаем информацию о пользователе
		JwtUserDetailsResponse userDetails = userService.getUserDetailsById(userId);
		if (userDetails == null) {
			log.warn("Пользователь с ID {} не найден", userId);
			return ResponseEntity.status(403)
								 .build();
		}

		log.info("Токен успешно валидирован и получены данные пользователя");
		return ResponseEntity.ok(userDetails);
	}
}
