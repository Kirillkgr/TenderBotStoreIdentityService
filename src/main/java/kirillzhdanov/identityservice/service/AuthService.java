package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.dto.LoginRequest;
import kirillzhdanov.identityservice.dto.LoginResponse;
import kirillzhdanov.identityservice.dto.UserRegistrationRequest;
import kirillzhdanov.identityservice.dto.UserResponse;
import kirillzhdanov.identityservice.exception.BadRequestException;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    public UserResponse registerUser(UserRegistrationRequest request) {
        // Проверяем, что пользователь с таким именем не существует
        if (userRepository.existsByUsername(request.getUsername())) {
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

    public LoginResponse login(LoginRequest request) {
        // Аутентифицируем пользователя
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // Генерируем JWT токен
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwt = jwtUtils.generateToken(userDetails);

        // Возвращаем токен
        return LoginResponse.builder()
                .token(jwt)
                .build();
    }
}
