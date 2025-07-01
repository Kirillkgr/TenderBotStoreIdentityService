package kirillzhdanov.identityservice.controller;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
