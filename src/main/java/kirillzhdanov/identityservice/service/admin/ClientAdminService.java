package kirillzhdanov.identityservice.service.admin;

import kirillzhdanov.identityservice.dto.client.ClientDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientAdminService {
    Page<ClientDto> findClients(Pageable pageable, String search, Long masterId);
}
