package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.dto.JwtUserDetailsResponse;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	/**
	 * Получает информацию о пользователе по ID для использования в JWT токене
	 *
	 * @param userId ID пользователя
	 * @return JwtUserDetailsResponse или null, если пользователь не найден
	 */
	public JwtUserDetailsResponse getUserDetailsById(Long userId) {

		Optional<User> userOptional = userRepository.findById(userId);

		if (userOptional.isEmpty()) {
			log.warn("Пользователь с ID {} не найден", userId);
			return null;
		}

		User user = userOptional.get();

		// Преобразуем роли в список строк
		List<String> roles = user.getRoles()
								 .stream()
								 .map(role -> role.getName()
												  .name())
								 .collect(Collectors.toList());

		// Преобразуем бренды в список ID
		List<Long> brandIds = user.getBrands()
								  .stream()
								  .map(brand -> brand.getId())
								  .collect(Collectors.toList());

		return JwtUserDetailsResponse.builder()
									 .userId(user.getId())
									 .username(user.getUsername())
									 .brandIds(brandIds)
									 .roles(roles)
									 .build();
	}

	public boolean existsByUsername(String username) {

		return userRepository.existsByUsername(username);
	}

	public User saveNewUser(User user) {
		// Создаем нового пользователя
		User userToSave = User.builder()
							  .username(user.getUsername())
							  .password(passwordEncoder.encode(user.getPassword()))
							  .brands(user.getBrands() == null ? new HashSet<>() : user.getBrands())
							  .roles(user.getRoles() == null ? new HashSet<>() : user.getRoles())
							  .build();
		userToSave = userRepository.save(userToSave);
		return userToSave;
	}

	public User save(User user) {

		return userRepository.save(user);
	}
}
