package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.dto.*;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.repository.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class BotService {

	@Autowired
	private BrandRepository brandRepository;

	public MessageResponse registerBot(BotRegistrationRequest request) {
		// Получаем текущего пользователя (если нужно, можно убрать)
		Authentication authentication = SecurityContextHolder.getContext()
															 .getAuthentication();
		UserDetails userDetails = (UserDetails) authentication.getPrincipal();
		String username = userDetails.getUsername();

		// Находим бренд по brandId
		Brand brand = brandRepository.findById(request.getBrandId())
									 .orElseThrow(() -> new ResourceNotFoundException("Бренд не найден"));

		// Сохраняем токен в бренд
		brand.setTelegramBotToken(request.getBotToken());
		brandRepository.save(brand);

		return MessageResponse.builder()
							  .message("Бот успешно зарегистрирован для бренда: " + brand.getName())
							  .build();
	}

	public MessageResponse startBot(BotRegistrationRequest request) {
		// Проверяем, что бренд с таким токеном существует
		Brand brand = brandRepository.findAll()
									 .stream()
									 .filter(b -> request.getBotToken()
														 .equals(b.getTelegramBotToken()))
									 .findFirst()
									 .orElseThrow(() -> new ResourceNotFoundException("Бот с таким токеном не найден"));
		//TODO: Start botService
		// Здесь будет логика запуска бота
		return MessageResponse.builder()
							  .message("Бот запущен для бренда: " + brand.getName())
							  .build();
	}
}
