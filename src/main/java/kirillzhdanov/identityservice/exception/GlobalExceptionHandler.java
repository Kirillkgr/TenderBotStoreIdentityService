package kirillzhdanov.identityservice.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {

		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult()
		  .getAllErrors()
		  .forEach(error -> {
			  String fieldName = ((FieldError) error).getField();
			  String errorMessage = error.getDefaultMessage();
			  errors.put(fieldName, errorMessage);
		  });
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							 .body(errors);
	}

	/**
	 * Стандартизированный 403 для RBAC-ошибок из контроллеров (RbacGuard -> AccessDeniedException).
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
		Map<String, String> error = new HashMap<>();
		error.put("message", ex.getMessage() != null ? ex.getMessage() : "Недостаточно прав");
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<Map<String, String>> handleBadCredentialsException() {

		Map<String, String> error = new HashMap<>();
		error.put("message", "Неверное имя пользователя или пароль");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
							 .body(error);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException ex) {

		Map<String, String> error = new HashMap<>();
		error.put("message", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
							 .body(error);
	}

	/**
	 * Маскировка чужих сущностей и отсутствующих записей: 404 вместо утечки информации.
	 */
	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleEntityNotFoundException(EntityNotFoundException ex) {
		Map<String, String> error = new HashMap<>();
		error.put("message", ex.getMessage() != null ? ex.getMessage() : "Ресурс не найден");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
		Map<String, String> error = new HashMap<>();
		error.put("message", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							 .body(error);
	}

	@ExceptionHandler(ResourceAlreadyExistsException.class)
	public ResponseEntity<Map<String, String>> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex) {
		Map<String, String> error = new HashMap<>();
		error.put("message", ex.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT)
							 .body(error);
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<Map<String, String>> handleBadRequestException(BadRequestException ex) {

		Map<String, String> error = new HashMap<>();
		error.put("message", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							 .body(error);
	}

	/**
	 * Fallback для аутентификационных ошибок (401), не перехваченных SecurityConfig.
	 */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException ex) {
		Map<String, String> error = new HashMap<>();
		error.put("message", ex.getMessage() != null ? ex.getMessage() : "Требуется аутентификация");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
	}

	@ExceptionHandler(TokenRefreshException.class)
	public ResponseEntity<Map<String, String>> handleTokenRefreshException(TokenRefreshException ex) {

		Map<String, String> error = new HashMap<>();
		error.put("message", ex.getMessage());
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
							 .body(error);
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {

		Map<String, String> error = new HashMap<>();
		error.put("message", ex.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							 .body(error);
	}
}
