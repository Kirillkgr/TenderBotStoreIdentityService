package kirillzhdanov.identityservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class CorsConfig {

    private final CorsProperties frontHost;

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^https?://" +                // http или https
                    "[\\w-]+(?:\\.[\\w-]+)*" +    // хост: один или более [буква/цифра/подчёркивание/дефис], опционально с «точка+метка»
                    "(?::([1-9]\\d{0,4}))?" +     // необязательный порт 1–99999
                    "(?:/.*)?$"                   // необязательный путь
    );
    private final int MAX_ORIGINS = 10;

    @Bean
    public CorsFilter corsFilter() {
        log.info("Установлено максимальное количество CORS-адресов: {}", MAX_ORIGINS);
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(validateCorsConfig());
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS","PATCH"));
        corsConfig.addAllowedHeader("*");
		corsConfig.setAllowCredentials(true);
		corsConfig.addExposedHeader("Authorization");
		corsConfig.addAllowedMethod("GET");
		corsConfig.addAllowedMethod("POST");
		corsConfig.addAllowedMethod("PUT");
		corsConfig.addAllowedMethod("PATCH");
		corsConfig.addAllowedMethod("DELETE");
		corsConfig.addAllowedMethod("OPTIONS");
		corsConfig.setMaxAge(3600L);
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsFilter(source);
    }

    private List<String> validateCorsConfig() {
        // Получаем сырой список из CorsProperties
        List<String> rawList = frontHost.getList();

        // 1. Проверяем, что список задан
        if (rawList == null || rawList.isEmpty()) {
            log.warn("⚠️ CORS запрещён: параметр 'front-service-cors.list' не указан или пуст.");
            return List.of();
        }

		// 2. Ограничиваем максимальное количество адресов
		if (rawList.size() > MAX_ORIGINS) {
			throw new IllegalStateException(
					String.format("Превышено максимальное количество CORS-адресов: %d > %d",
							rawList.size(), MAX_ORIGINS)
			);
		}

		// 3. Разбираем и разделяем на валидные и невалидные
		var partition = rawList.stream()
							   .map(String::trim)
							   .filter(s -> !s.isEmpty())
							   .collect(Collectors.partitioningBy(this::isValidUrl));

		List<String> valid = partition.get(true);
		List<String> invalid = partition.get(false);

		// 4. Логируем невалидные адреса как ошибку
		if (!invalid.isEmpty()) {
			log.error("❌ Неверный формат адресов CORS: {}", invalid);
		}

		// 5. Логируем валидные адреса для подтверждения
		if (!valid.isEmpty()) {
			log.info("✅ Разрешённые адреса CORS: {}", valid);
		}

		// 6. Возвращаем неизменяемый список валидных адресов
		return List.copyOf(valid);
	}


	public boolean isValidUrl(String url) {
		if (url == null || url.isBlank()) return false;
		return URL_PATTERN.matcher(url.trim())
						  .matches();
	}
}