package kirillzhdanov.identityservice.service.impl.newImpl;

import kirillzhdanov.identityservice.dto.JwtUserDetailsResponse;
import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.repository.UserRepository;
import kirillzhdanov.identityservice.service.UserService;
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
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    /**
     * Получает информацию о пользователе по ID для использования в JWT токене
     *
     * @param userId ID пользователя
     * @return JwtUserDetailsResponse или null, если пользователь не найден
     */
    @Override
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
                .map(Brand::getId)
                .collect(Collectors.toList());

        return JwtUserDetailsResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .brandIds(brandIds)
                .roles(roles)
                .build();
    }

    @Override
    public boolean existsByUsername(String username) {

        return userRepository.existsByUsername(username);
    }

    @Override
    public User saveNewUser(User user) {
        // Создаем нового пользователя
        User userToSave =   User.builder()
                .username(user.getUsername())
                  .password(passwordEncoder.encode(user.getPassword()))

                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .patronymic(user.getPatronymic())
                .dateOfBirth(user.getDateOfBirth())
                .email(user.getEmail())
                .phone(user.getPhone())
                .emailVerified(user.isEmailVerified())

                .brands(new HashSet<>())
                .roles(new HashSet<>())
                .build();

        userToSave = userRepository.save(userToSave);
        return userToSave;
    }

    @Override
    public User save(User user) {

        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
