package kirillzhdanov.identityservice.controller;

import jakarta.validation.Valid;
import kirillzhdanov.identityservice.dto.*;
import kirillzhdanov.identityservice.service.BotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для управления Telegram-ботами.
 */
@RestController
@RequestMapping("/bot")
public class BotController {

	@Autowired
	private BotService botService;

	/**
	 * Регистрация нового Telegram-бота.
	 *
	 * @param request данные для регистрации бота
	 * @return {@code 200 OK} – бот успешно зарегистрирован;
	 * {@code 400 Bad Request} – некорректные данные;
	 * {@code 409 Conflict} – бот с таким токеном уже существует.
	 */
	@PostMapping("/register")
	public ResponseEntity<MessageResponse> registerBot(@Valid @RequestBody BotRegistrationRequest request) {

		MessageResponse response = botService.registerBot(request);
		return ResponseEntity.ok(response);
	}

	/**
	 * Запуск зарегистрированного Telegram-бота.
	 *
	 * @param request данные для запуска бота
	 * @return {@code 200 OK} – бот успешно запущен;
	 * {@code 400 Bad Request} – некорректные данные;
	 * {@code 404 Not Found} – бот не найден.
	 */
	@PostMapping("/start")
	public ResponseEntity<MessageResponse> startBot(@Valid @RequestBody BotRegistrationRequest request) {

		MessageResponse response = botService.startBot(request);
		return ResponseEntity.ok(response);
	}
}
