package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.dto.LoginRequest;
import kirillzhdanov.identityservice.dto.LoginResponse;
import kirillzhdanov.identityservice.dto.TokenRefreshRequest;
import kirillzhdanov.identityservice.dto.TokenRefreshResponse;
import kirillzhdanov.identityservice.dto.UserRegistrationRequest;
import kirillzhdanov.identityservice.dto.UserResponse;
import kirillzhdanov.identityservice.exception.BadRequestException;
import kirillzhdanov.identityservice.exception.TokenRefreshException;
import kirillzhdanov.identityservice.model.Token;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	private final AuthenticationManager authenticationManager;

	private final JwtUtils jwtUtils;

	private final TokenService tokenService;

	@Transactional
	public UserResponse registerUser(UserRegistrationRequest request){
		// Проверяем, что пользователь с таким именем не существует
		if(userRepository.existsByUsername(request.getUsername())) {
			throw new BadRequestException("Пользователь с таким именем уже существует");
		}

		// Создаем нового пользователя
		User user = User.builder()
				.username(request.getUsername())
				.password(passwordEncoder.encode(request.getPassword()))
				.build();

		// Сохраняем пользователя
		User savedUser = userRepository.save(user);

		// Возвращаем ответ
		return UserResponse.builder()
				.id(savedUser.getId())
				.username(savedUser.getUsername())
				.build();
	}

	@Transactional
	public LoginResponse login(LoginRequest request){
		// Аутентифицируем пользователя
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
		);

		// Получаем данные пользователя
		UserDetails userDetails = (UserDetails) authentication.getPrincipal();
		User user = userRepository.findByUsername(userDetails.getUsername())
				.orElseThrow(()->new BadRequestException("Пользователь не найден"));

		// Отзываем все существующие токены пользователя (опционально)
		tokenService.revokeAllUserTokens(user);

		// Генерируем новые токены
		String accessToken = jwtUtils.generateAccessToken(userDetails);
		String refreshToken = jwtUtils.generateRefreshToken(userDetails);

		// Сохраняем токены в базу данных
		tokenService.saveToken(accessToken, Token.TokenType.ACCESS, user);
		tokenService.saveToken(refreshToken, Token.TokenType.REFRESH, user);

		// Возвращаем токены
		return LoginResponse.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.build();
	}

	@Transactional
	public TokenRefreshResponse refreshToken(TokenRefreshRequest request){

		String requestRefreshToken = request.getRefreshToken();

		// Проверяем, существует ли токен и действителен ли он
		Optional<Token> tokenOptional = tokenService.findByToken(requestRefreshToken);

		if(tokenOptional.isEmpty() || !tokenOptional.get().isValid()) {
			throw new TokenRefreshException("Токен обновления недействителен или истек");
		}

		Token refreshToken = tokenOptional.get();

		// Проверяем тип токена
		if(refreshToken.getTokenType() != Token.TokenType.REFRESH) {
			throw new TokenRefreshException("Неверный тип токена");
		}

		// Получаем пользователя
		User user = refreshToken.getUser();
		UserDetails userDetails = org.springframework.security.core.userdetails.User
				.withUsername(user.getUsername())
				.password(user.getPassword())
				.authorities("USER")
				.build();

		// Генерируем новый токен доступа
		String newAccessToken = jwtUtils.generateAccessToken(userDetails);

		// Сохраняем новый токен доступа
		tokenService.saveToken(newAccessToken, Token.TokenType.ACCESS, user);

		// Возвращаем новый токен доступа и тот же токен обновления
		return TokenRefreshResponse.builder()
				.accessToken(newAccessToken)
				.refreshToken(requestRefreshToken)
				.build();
	}

	@Transactional
	public void revokeToken(String token){

		tokenService.revokeToken(token);
	}

	@Transactional
	public void revokeAllUserTokens(String username){

		User user = userRepository.findByUsername(username)
				.orElseThrow(()->new BadRequestException("Пользователь не найден"));
		tokenService.revokeAllUserTokens(user);
	}
}
