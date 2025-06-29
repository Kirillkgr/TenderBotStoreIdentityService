package kirillzhdanov.identityservice.exception;

import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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
