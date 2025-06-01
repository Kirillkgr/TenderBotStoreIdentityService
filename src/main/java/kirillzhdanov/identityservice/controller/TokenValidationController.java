package kirillzhdanov.identityservice.controller;

import kirillzhdanov.identityservice.dto.JwtUserDetailsResponse;
import kirillzhdanov.identityservice.security.JwtUtils;
import kirillzhdanov.identityservice.service.TokenService;
import kirillzhdanov.identityservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/validate")
@RequiredArgsConstructor
@Slf4j
public class TokenValidationController {

    private final TokenService tokenService;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    /**
     * Валидирует токен и возвращает пустой ответ с кодом 200, если токен действителен
     */
    @PostMapping
    public ResponseEntity<Void> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Получен запрос с некорректным заголовком Authorization");
            return ResponseEntity.status(403).build();
        }

        String token = authHeader.substring(7);
        
        // Проверяем подпись токена
        boolean isSignatureValid = jwtUtils.validateTokenSignature(token);
        if (!isSignatureValid) {
            log.warn("Токен имеет недействительную подпись");
            return ResponseEntity.status(403).build();
        }
        
        // Проверяем, не отозван ли токен
        boolean isTokenValid = tokenService.isTokenValid(token);
        if (!isTokenValid) {
            log.warn("Токен отозван или не найден в базе данных");
            return ResponseEntity.status(403).build();
        }

        log.info("Токен успешно валидирован");
        return ResponseEntity.ok().build();
    }
    
    /**
     * Валидирует токен и возвращает информацию о пользователе, если токен действителен
     */
    @PostMapping("/user-details")
    public ResponseEntity<JwtUserDetailsResponse> validateTokenAndGetUserDetails(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Получен запрос с некорректным заголовком Authorization");
            return ResponseEntity.status(403).build();
        }

        String token = authHeader.substring(7);
        
        // Проверяем подпись токена
        boolean isSignatureValid = jwtUtils.validateTokenSignature(token);
        if (!isSignatureValid) {
            log.warn("Токен имеет недействительную подпись");
            return ResponseEntity.status(403).build();
        }
        
        // Проверяем, не отозван ли токен
        boolean isTokenValid = tokenService.isTokenValid(token);
        if (!isTokenValid) {
            log.warn("Токен отозван или не найден в базе данных");
            return ResponseEntity.status(403).build();
        }
        
        // Получаем ID пользователя из токена
        Long userId = jwtUtils.extractUserId(token);
        if (userId == null) {
            log.warn("Не удалось извлечь ID пользователя из токена");
            return ResponseEntity.status(403).build();
        }
        
        // Получаем информацию о пользователе
        JwtUserDetailsResponse userDetails = userService.getUserDetailsById(userId);
        if (userDetails == null) {
            log.warn("Пользователь с ID {} не найден", userId);
            return ResponseEntity.status(403).build();
        }

        log.info("Токен успешно валидирован и получены данные пользователя");
        return ResponseEntity.ok(userDetails);
    }
}
