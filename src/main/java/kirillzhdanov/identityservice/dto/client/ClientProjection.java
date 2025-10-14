package kirillzhdanov.identityservice.dto.client;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ClientProjection {
    Long getId();

    String getFirstName();

    String getLastName();

    String getPatronymic();

    String getEmail();

    String getPhone();

    LocalDate getDateOfBirth();

    LocalDateTime getLastOrderAt();

    Long getLastOrderBrandId();

    String getLastOrderBrand();
}
