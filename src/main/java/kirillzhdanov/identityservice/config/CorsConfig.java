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
        corsConfig.setAllowedOriginPatterns(validateCorsConfig());
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        corsConfig.addAllowedHeader("*");
        corsConfig.addExposedHeader("Authorization");

        corsConfig.setMaxAge(3600L);
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsFilter(source);
    }

    private List<String> validateCorsConfig() {
        // 0) Берём список из настроек
        List<String> rawList = frontHost.getList();
        if (rawList == null || rawList.isEmpty()) {
            log.warn("⚠️ CORS запрещён: параметр 'front-service-cors.list' не указан или пуст.");
            return List.of();
        }

        // Регулярка для wildcard-масок: http(s)://*.domain[:port]
        final Pattern WILDCARD = Pattern.compile("^https?://\\*\\.[\\w-]+(?:\\.[\\w-]+)*(?::([1-9]\\d{0,4}))?$");

        // 1) Нормализация и разделение
        var normalized = rawList.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                // срежем все хвостовые слэши
                .map(s -> s.replaceAll("/+$", ""))
                .toList();

        List<String> exact = new java.util.ArrayList<>();
        List<String> patterns = new java.util.ArrayList<>();
        List<String> invalid = new java.util.ArrayList<>();

        for (String origin : normalized) {
            if (origin.contains("*")) {
                if (WILDCARD.matcher(origin).matches()) {
                    patterns.add(origin);
                } else {
                    invalid.add(origin);
                }
            } else {
                if (isValidUrl(origin)) {
                    exact.add(origin);
                } else {
                    invalid.add(origin);
                }
            }
        }

        if (!invalid.isEmpty()) {
            log.error("❌ Неверный формат адресов CORS: {}", invalid);
        }

        // 2) Логи по валидным спискам
        if (!exact.isEmpty()) {
            log.info("✅ Разрешённые точные CORS: {}", exact);
        }
        if (!patterns.isEmpty()) {
            log.info("✅ Разрешённые шаблоны CORS: {}", patterns);
        }

        // 3) Возвращаем объединённый список
        List<String> all = new java.util.ArrayList<>(exact.size() + patterns.size());
        all.addAll(exact);
        all.addAll(patterns);
        return java.util.List.copyOf(all);
    }
//    private List<String> validateCorsConfig() {
//        // Получаем сырой список из CorsProperties
//        List<String> rawList = frontHost.getList();
//
//        // 1. Проверяем, что список задан
//        if (rawList == null || rawList.isEmpty()) {
//            log.warn("⚠️ CORS запрещён: параметр 'front-service-cors.list' не указан или пуст.");
//            return List.of();
//        }
//
//		// 2. Ограничиваем максимальное количество адресов
//		if (rawList.size() > MAX_ORIGINS) {
//			throw new IllegalStateException(
//					String.format("Превышено максимальное количество CORS-адресов: %d > %d",
//							rawList.size(), MAX_ORIGINS)
//			);
//		}
//
//		// 3. Разбираем и разделяем на валидные и невалидные
//		var partition = rawList.stream()
//							   .map(String::trim)
//							   .filter(s -> !s.isEmpty())
//							   .collect(Collectors.partitioningBy(this::isValidUrl));
//
//		List<String> valid = partition.get(true);
//		List<String> invalid = partition.get(false);
//
//		// 4. Логируем невалидные адреса как ошибку
//		if (!invalid.isEmpty()) {
//			log.error("❌ Неверный формат адресов CORS: {}", invalid);
//		}
//
//		// 5. Логируем валидные адреса для подтверждения
//		if (!valid.isEmpty()) {
//			log.info("✅ Разрешённые адреса CORS: {}", valid);
//		}
//
//		// 6. Возвращаем неизменяемый список валидных адресов
//		return List.copyOf(valid);
//	}


    public boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) return false;
        return URL_PATTERN.matcher(url.trim())
                .matches();
    }
}