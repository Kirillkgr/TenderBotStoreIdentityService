package kirillzhdanov.identityservice.util;

import kirillzhdanov.identityservice.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
@RequiredArgsConstructor
public class Base64Utils {
	public static LoginRequest getUsernameAndPassword(String authHeader) {
		// Декодируем и парсим логин/пароль
		String base64Credentials = authHeader.substring("Basic ".length())
											 .trim();
		String[] credentials = new String(Base64.getDecoder()
												.decode(base64Credentials)).split(":", 2);

		// Создаем LoginRequest
		LoginRequest usernameAndPassword = new LoginRequest(credentials[0],
				credentials.length > 1 ? credentials[1] : "");
		return usernameAndPassword;
	}
}
