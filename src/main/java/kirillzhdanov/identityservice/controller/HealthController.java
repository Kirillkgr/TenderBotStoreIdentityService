package kirillzhdanov.identityservice.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/status")
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class HealthController {

	@GetMapping
	ResponseEntity<Void> health() {
		return ResponseEntity.ok()
							 .build();
	}
}
