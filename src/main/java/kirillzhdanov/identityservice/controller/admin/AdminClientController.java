package kirillzhdanov.identityservice.controller.admin;

import kirillzhdanov.identityservice.dto.client.ClientDto;
import kirillzhdanov.identityservice.service.admin.ClientAdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/clients")
@PreAuthorize("hasAnyRole('ADMIN','OWNER')")
public class AdminClientController {

    private final ClientAdminService clientAdminService;

    public AdminClientController(ClientAdminService clientAdminService) {
        this.clientAdminService = clientAdminService;
    }

    @GetMapping
    public ResponseEntity<Page<ClientDto>> getClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long masterId
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<ClientDto> result = clientAdminService.findClients(pageable, search, masterId);
        return ResponseEntity.ok(result);
    }
}
