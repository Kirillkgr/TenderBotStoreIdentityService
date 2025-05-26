package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.dto.BotRegistrationRequest;
import kirillzhdanov.identityservice.dto.MessageResponse;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class BotService {

	@Autowired
	private UserRepository userRepository;

	public MessageResponse registerBot(BotRegistrationRequest request){
		// Получаем текущего пользователя
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetails userDetails = (UserDetails) authentication.getPrincipal();
		String username = userDetails.getUsername();

		// Ищем пользователя в базе
		User user = userRepository.findByUsername(username)
				.orElseThrow(()->new ResourceNotFoundException("Пользователь не найден"));

		// Обновляем токен бота
		user.setTelegramBotToken(request.getBotToken());
		userRepository.save(user);

		return MessageResponse.builder()
				.message("Бот успешно зарегистрирован")
				.build();
	}

	public MessageResponse startBot(BotRegistrationRequest request){
		// Проверяем, что бот с таким токеном существует
		User user = userRepository.findByTelegramBotToken(request.getBotToken())
				.orElseThrow(()->new ResourceNotFoundException("Бот с таким токеном не найден"));
		//TODO: Start botServise
		// Здесь будет логика запуска бота
		// В реальном приложении здесь может быть вызов другого сервиса или отправка события в Kafka

		return MessageResponse.builder()
				.message("Бот запущен")
				.build();
	}
}
